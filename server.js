const express = require('express');
const cors = require('cors');
const { TelegramClient } = require('telegram');
const { StringSession } = require('telegram/sessions');
const { Api } = require('telegram/tl');
const { computeCheck } = require('telegram/Password');
const path = require('path');

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static('.'));

// Store sessions per user (in production, use a database)
const sessions = new Map();

// Rate limiting helper
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Get or create client for a session
async function getClient(sessionId) {
    if (!sessions.has(sessionId)) {
        return null;
    }
    return sessions.get(sessionId).client;
}

// ==================== API ENDPOINTS ====================

// Initialize client with API credentials
app.post('/api/init', async (req, res) => {
    try {
        const { apiId, apiHash, sessionString } = req.body;

        const stringSession = new StringSession(sessionString || '');
        const client = new TelegramClient(stringSession, parseInt(apiId), apiHash, {
            connectionRetries: 5,
        });

        await client.connect();

        // Generate a session ID
        const sessionId = 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);

        // Check if already authorized
        let isAuthorized = false;
        let me = null;
        try {
            me = await client.getMe();
            isAuthorized = true;
        } catch (e) {
            isAuthorized = false;
        }

        sessions.set(sessionId, {
            client,
            apiId: parseInt(apiId),
            apiHash,
            phone: null,
            phoneCodeHash: null,
            me
        });

        res.json({
            success: true,
            sessionId,
            isAuthorized,
            user: me ? { phone: me.phone, username: me.username, firstName: me.firstName } : null
        });
    } catch (error) {
        console.error('Init error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Send verification code
app.post('/api/send-code', async (req, res) => {
    try {
        const { sessionId, phone } = req.body;
        const session = sessions.get(sessionId);

        if (!session) {
            return res.status(400).json({ success: false, error: 'Session not found' });
        }

        const result = await session.client.sendCode(
            { apiId: session.apiId, apiHash: session.apiHash },
            phone
        );

        session.phone = phone;
        session.phoneCodeHash = result.phoneCodeHash;

        res.json({ success: true, phoneCodeHash: result.phoneCodeHash });
    } catch (error) {
        console.error('Send code error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Sign in with code
app.post('/api/sign-in', async (req, res) => {
    try {
        const { sessionId, code } = req.body;
        const session = sessions.get(sessionId);

        if (!session) {
            return res.status(400).json({ success: false, error: 'Session not found' });
        }

        try {
            await session.client.invoke(new Api.auth.SignIn({
                phoneNumber: session.phone,
                phoneCodeHash: session.phoneCodeHash,
                phoneCode: code,
            }));

            const me = await session.client.getMe();
            session.me = me;

            // Save session string for persistence
            const sessionString = session.client.session.save();

            res.json({
                success: true,
                sessionString,
                user: { phone: me.phone, username: me.username, firstName: me.firstName }
            });
        } catch (error) {
            if (error.errorMessage === 'SESSION_PASSWORD_NEEDED') {
                return res.json({ success: false, needs2FA: true });
            }
            throw error;
        }
    } catch (error) {
        console.error('Sign in error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Sign in with 2FA password
app.post('/api/sign-in-2fa', async (req, res) => {
    try {
        const { sessionId, password } = req.body;
        const session = sessions.get(sessionId);

        if (!session) {
            return res.status(400).json({ success: false, error: 'Session not found' });
        }

        const passwordSrpResult = await session.client.invoke(new Api.account.GetPassword());
        const passwordSrpCheck = await computeCheck(passwordSrpResult, password);

        await session.client.invoke(new Api.auth.CheckPassword({
            password: passwordSrpCheck,
        }));

        const me = await session.client.getMe();
        session.me = me;

        const sessionString = session.client.session.save();

        res.json({
            success: true,
            sessionString,
            user: { phone: me.phone, username: me.username, firstName: me.firstName }
        });
    } catch (error) {
        console.error('2FA error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Get dialogs (chats)
app.get('/api/dialogs', async (req, res) => {
    try {
        const { sessionId } = req.query;
        const session = sessions.get(sessionId);

        if (!session) {
            return res.status(400).json({ success: false, error: 'Session not found' });
        }

        const dialogs = await session.client.getDialogs({ limit: 50 });

        const result = dialogs.map(dialog => ({
            id: dialog.id.toString(),
            name: dialog.title || dialog.name || 'Unknown',
            type: dialog.isGroup ? 'group' : dialog.isChannel ? 'channel' : 'private',
            avatar: (dialog.title || dialog.name || 'U').substring(0, 2).toUpperCase(),
            lastMessage: dialog.message?.message || '',
            messageCount: 0,
            date: dialog.date,
            time: dialog.date ? new Date(dialog.date * 1000).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '',
        }));

        res.json({ success: true, dialogs: result });
    } catch (error) {
        console.error('Get dialogs error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Get messages for a chat
app.get('/api/messages', async (req, res) => {
    try {
        const { sessionId, chatId, limit = 100, offsetId = 0 } = req.query;
        const session = sessions.get(sessionId);

        if (!session) {
            return res.status(400).json({ success: false, error: 'Session not found' });
        }

        // Get the entity for this chat
        const dialogs = await session.client.getDialogs({ limit: 50 });
        const dialog = dialogs.find(d => d.id.toString() === chatId);

        if (!dialog) {
            return res.status(404).json({ success: false, error: 'Chat not found' });
        }

        // Get messages from this user only
        const messages = await session.client.getMessages(dialog.entity, {
            limit: parseInt(limit),
            offsetId: parseInt(offsetId),
            fromUser: 'me',
        });

        const result = messages.map(msg => ({
            id: msg.id,
            text: msg.message || (msg.media ? '[Media]' : '[No text]'),
            date: new Date(msg.date * 1000).toLocaleString(),
            sender: 'You',
            type: msg.photo ? 'photo' : msg.video ? 'video' : msg.document ? 'file' : 'text',
        }));

        res.json({ success: true, messages: result });
    } catch (error) {
        console.error('Get messages error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Batch scan for my messages
app.post('/api/batch-scan', async (req, res) => {
    try {
        const { sessionId, chatIds } = req.body;
        const session = sessions.get(sessionId);

        if (!session) {
            return res.status(400).json({ success: false, error: 'Session not found' });
        }

        const counts = {};
        const BATCH_SIZE = 10; // Parallel requests

        // Helper to process a single ID
        const checkChat = async (chatId) => {
            try {
                // Fetch 0 messages but get the TOTAL count
                const result = await session.client.getMessages(chatId, {
                    limit: 1, // Need at least 1 to get total sometimes, or 0. Using 1 is safe.
                    fromUser: 'me',
                });
                // result.total is the count of messages matching filter
                return { id: chatId, count: result.total || 0 };
            } catch (err) {
                // console.warn(`Scan error for ${chatId}:`, err.message);
            }
            return { id: chatId, count: 0 };
        };

        // Process in batches
        for (let i = 0; i < chatIds.length; i += BATCH_SIZE) {
            const chunk = chatIds.slice(i, i + BATCH_SIZE);
            const promises = chunk.map(id => checkChat(id));
            const results = await Promise.all(promises);

            results.forEach(({ id, count }) => {
                if (count > 0) counts[id] = count;
            });

            // Tiny sleep between batches
            await new Promise(r => setTimeout(r, 50));
        }

        res.json({ success: true, counts });
    } catch (error) {
        console.error('Batch scan error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Delete messages
app.post('/api/delete-messages', async (req, res) => {
    try {
        const { sessionId, chatId, messageIds, revoke = true } = req.body;
        const session = sessions.get(sessionId);

        if (!session) {
            return res.status(400).json({ success: false, error: 'Session not found' });
        }

        // Get the entity for this chat
        const dialogs = await session.client.getDialogs({ limit: 50 });
        const dialog = dialogs.find(d => d.id.toString() === chatId);

        if (!dialog) {
            return res.status(404).json({ success: false, error: 'Chat not found' });
        }

        try {
            await session.client.deleteMessages(dialog.entity, messageIds, { revoke });
            res.json({ success: true, deleted: messageIds.length });
        } catch (err) {
            if (err.seconds) {
                // Return FloodWait to client so it can handle the pause/UI
                console.log(`FloodWait encountered: ${err.seconds} seconds`);
                return res.json({ success: false, floodWait: err.seconds });
            } else {
                throw err;
            }
        }
    } catch (error) {
        console.error('Delete messages error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Logout
app.post('/api/logout', async (req, res) => {
    try {
        const { sessionId } = req.body;
        const session = sessions.get(sessionId);

        if (session) {
            try {
                await session.client.disconnect();
            } catch (e) { }
            sessions.delete(sessionId);
        }

        res.json({ success: true });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// Serve the main HTML file
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'telegram-cleaner-modern.html'));
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`
╔════════════════════════════════════════════════════════════╗
║           Telegram Cleaner Backend Running                 ║
╠════════════════════════════════════════════════════════════╣
║  Server: http://localhost:${PORT}                             ║
║  API:    http://localhost:${PORT}/api                         ║
╚════════════════════════════════════════════════════════════╝
    `);
});
