/**
 * Telegram Cleaner Backend Server with TDLib Integration
 *
 * This server provides API endpoints that the Electron app connects to.
 * It handles real communication with Telegram's API via TDLib.
 *
 * Run with: node backend-server.js
 */

const express = require('express');
const cors = require('cors');
const path = require('path');
const tdl = require('tdl');
const { getTdjson } = require('prebuilt-tdlib');

// Configure tdl to use prebuilt TDLib
console.log('=== TDLib Configuration ===');
try {
    const tdjsonPath = getTdjson();
    console.log('TDLib path:', tdjsonPath);
    tdl.configure({ tdjson: tdjsonPath });
    console.log('✓ TDLib configured successfully');
} catch (error) {
    console.error('✗ ERROR configuring TDLib:', error);
    console.error('Stack:', error.stack);
    process.exit(1);
}

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Serve static files (for deployment)
app.use(express.static(__dirname));

// Serve the main HTML file at root
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'telegram-cleaner-modern.html'));
});

// Session storage - maps sessionId to TDLib client instance
const sessions = new Map();

// Helper function to get or create TDLib client
async function getOrCreateClient(sessionId, apiId, apiHash) {
    if (sessions.has(sessionId)) {
        console.log('Reusing existing session:', sessionId);
        return sessions.get(sessionId);
    }

    console.log('Creating new TDLib client for session:', sessionId);

    try {
        const client = tdl.createClient({
            apiId: parseInt(apiId),
            apiHash: apiHash,
            databaseDirectory: path.join(__dirname, '.tdlib', sessionId),
            filesDirectory: path.join(__dirname, '.tdlib', sessionId, 'files'),
            verbosity: 2,
            useTestDc: false,
            tdlibParameters: {
                use_message_database: true,
                use_secret_chats: false,
                system_language_code: 'en',
                device_model: 'Desktop',
                application_version: '1.0'
            }
        });

        client.on('error', (error) => {
            console.error('TDLib client error:', error);
        });

        client.on('update', (update) => {
            console.log('TDLib update:', update._);
        });

        const sessionData = {
            client,
            apiId,
            apiHash,
            isAuthorized: false,
            phoneNumber: null
        };

        sessions.set(sessionId, sessionData);
        console.log('✓ TDLib client created successfully');
        return sessionData;
    } catch (error) {
        console.error('✗ Failed to create TDLib client:', error);
        console.error('Stack:', error.stack);
        throw new Error(`Failed to initialize Telegram client: ${error.message}`);
    }
}

// Helper function to wait for authorization
async function waitForAuthorization(client, timeout = 30000) {
    const startTime = Date.now();

    while (Date.now() - startTime < timeout) {
        try {
            const state = await client.invoke({
                _: 'getAuthorizationState'
            });

            if (state._ === 'authorizationStateReady') {
                return true;
            }

            if (state._ === 'authorizationStateClosed') {
                throw new Error('Authorization failed: connection closed');
            }

            await new Promise(resolve => setTimeout(resolve, 100));
        } catch (error) {
            if (error.message.includes('AUTHORIZATION_STATE')) {
                continue;
            }
            throw error;
        }
    }

    return false;
}

// Health check endpoint
app.get('/api/health', (req, res) => {
    res.json({
        status: 'ok',
        message: 'Backend server is running',
        mode: 'TDLib Integration Active'
    });
});

// Initialize Telegram session
app.post('/api/init', async (req, res) => {
    try {
        const { apiId, apiHash, sessionString } = req.body;

        if (!apiId || !apiHash) {
            return res.status(400).json({
                success: false,
                error: 'API ID and API Hash are required'
            });
        }

        const sessionId = Date.now().toString();
        const sessionData = await getOrCreateClient(sessionId, apiId, apiHash);

        // Check current authorization state
        const authState = await sessionData.client.invoke({
            _: 'getAuthorizationState'
        });

        let isAuthorized = authState._ === 'authorizationStateReady';
        let user = null;

        if (isAuthorized) {
            try {
                user = await sessionData.client.invoke({ _: 'getMe' });
                sessionData.isAuthorized = true;
            } catch (error) {
                console.log('Error getting user info:', error.message);
                isAuthorized = false;
            }
        }

        res.json({
            success: true,
            sessionId,
            isAuthorized,
            user: user ? {
                id: user.id.toString(),
                firstName: user.first_name,
                lastName: user.last_name,
                phone: user.phone_number,
                username: user.usernames?.active_usernames?.[0] || null
            } : null
        });

    } catch (error) {
        console.error('Init error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Send verification code
app.post('/api/send-code', async (req, res) => {
    try {
        const { sessionId, phone } = req.body;

        if (!sessions.has(sessionId)) {
            return res.status(400).json({
                success: false,
                error: 'Invalid session ID'
            });
        }

        const sessionData = sessions.get(sessionId);
        sessionData.phoneNumber = phone;

        // Set authentication phone number
        await sessionData.client.invoke({
            _: 'setAuthenticationPhoneNumber',
            phone_number: phone,
            settings: {
                _: 'phoneNumberAuthenticationSettings',
                allow_flash_call: false,
                allow_missed_call: false,
                is_current_phone_number: false,
                allow_sms_retriever_api: false
            }
        });

        res.json({
            success: true,
            message: `Code sent to ${phone}`
        });

    } catch (error) {
        console.error('Send code error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Sign in with code
app.post('/api/sign-in', async (req, res) => {
    try {
        const { sessionId, code } = req.body;

        if (!sessions.has(sessionId)) {
            return res.status(400).json({
                success: false,
                error: 'Invalid session ID'
            });
        }

        const sessionData = sessions.get(sessionId);

        // Check authentication code
        await sessionData.client.invoke({
            _: 'checkAuthenticationCode',
            code: code.toString()
        });

        // Wait for authorization to complete
        const isReady = await waitForAuthorization(sessionData.client);

        if (!isReady) {
            return res.status(500).json({
                success: false,
                error: 'Authorization timeout'
            });
        }

        // Get user info
        const user = await sessionData.client.invoke({ _: 'getMe' });
        sessionData.isAuthorized = true;

        res.json({
            success: true,
            user: {
                id: user.id.toString(),
                firstName: user.first_name,
                lastName: user.last_name,
                phone: user.phone_number,
                username: user.usernames?.active_usernames?.[0] || null
            }
        });

    } catch (error) {
        console.error('Sign in error:', error);

        // Check if 2FA is required
        if (error.message.includes('PASSWORD_HASH_INVALID') || error.message.includes('SESSION_PASSWORD_NEEDED')) {
            return res.json({
                success: false,
                needs2FA: true,
                error: 'Two-factor authentication required'
            });
        }

        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Sign in with 2FA password
app.post('/api/sign-in-2fa', async (req, res) => {
    try {
        const { sessionId, password } = req.body;

        if (!sessions.has(sessionId)) {
            return res.status(400).json({
                success: false,
                error: 'Invalid session ID'
            });
        }

        const sessionData = sessions.get(sessionId);

        // Check authentication password
        await sessionData.client.invoke({
            _: 'checkAuthenticationPassword',
            password: password
        });

        // Wait for authorization to complete
        const isReady = await waitForAuthorization(sessionData.client);

        if (!isReady) {
            return res.status(500).json({
                success: false,
                error: 'Authorization timeout'
            });
        }

        // Get user info
        const user = await sessionData.client.invoke({ _: 'getMe' });
        sessionData.isAuthorized = true;

        res.json({
            success: true,
            user: {
                id: user.id.toString(),
                firstName: user.first_name,
                lastName: user.last_name,
                phone: user.phone_number,
                username: user.usernames?.active_usernames?.[0] || null
            }
        });

    } catch (error) {
        console.error('2FA sign in error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Get dialogs/chats
app.get('/api/dialogs', async (req, res) => {
    try {
        const { sessionId } = req.query;

        if (!sessions.has(sessionId)) {
            return res.status(400).json({
                success: false,
                error: 'Invalid session ID'
            });
        }

        const sessionData = sessions.get(sessionId);

        if (!sessionData.isAuthorized) {
            return res.status(401).json({
                success: false,
                error: 'Not authorized'
            });
        }

        // Get chats
        const chats = await sessionData.client.invoke({
            _: 'getChats',
            chat_list: { _: 'chatListMain' },
            limit: 50
        });

        const dialogs = [];

        for (const chatId of chats.chat_ids) {
            try {
                const chat = await sessionData.client.invoke({
                    _: 'getChat',
                    chat_id: chatId
                });

                dialogs.push({
                    id: chatId.toString(),
                    name: chat.title,
                    type: chat.type._ === 'chatTypePrivate' ? 'private' :
                          chat.type._ === 'chatTypeBasicGroup' || chat.type._ === 'chatTypeSupergroup' ? 'group' : 'channel',
                    lastMessage: chat.last_message?.content?.text?.text || '',
                    unreadCount: chat.unread_count || 0,
                    messageCount: 0 // Will be counted separately if needed
                });
            } catch (error) {
                console.log(`Error getting chat ${chatId}:`, error.message);
            }
        }

        res.json({
            success: true,
            dialogs
        });

    } catch (error) {
        console.error('Get dialogs error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Get messages from a chat
app.get('/api/messages', async (req, res) => {
    try {
        const { sessionId, chatId, offsetId = 0, limit = 100 } = req.query;

        if (!sessions.has(sessionId)) {
            return res.status(400).json({
                success: false,
                error: 'Invalid session ID'
            });
        }

        const sessionData = sessions.get(sessionId);

        if (!sessionData.isAuthorized) {
            return res.status(401).json({
                success: false,
                error: 'Not authorized'
            });
        }

        // Get chat history
        const history = await sessionData.client.invoke({
            _: 'getChatHistory',
            chat_id: parseInt(chatId),
            from_message_id: parseInt(offsetId),
            offset: 0,
            limit: parseInt(limit)
        });

        const messages = history.messages.map(msg => ({
            id: msg.id.toString(),
            chatId: msg.chat_id.toString(),
            content: msg.content?.text?.text || '[Media]',
            date: msg.date * 1000, // Convert to milliseconds
            isOutgoing: msg.is_outgoing
        }));

        res.json({
            success: true,
            messages
        });

    } catch (error) {
        console.error('Get messages error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Delete messages
app.post('/api/delete-messages', async (req, res) => {
    try {
        const { sessionId, chatId, messageIds, revoke } = req.body;

        if (!sessions.has(sessionId)) {
            return res.status(400).json({
                success: false,
                error: 'Invalid session ID'
            });
        }

        const sessionData = sessions.get(sessionId);

        if (!sessionData.isAuthorized) {
            return res.status(401).json({
                success: false,
                error: 'Not authorized'
            });
        }

        let deletedCount = 0;

        for (const messageId of messageIds) {
            try {
                await sessionData.client.invoke({
                    _: 'deleteMessages',
                    chat_id: parseInt(chatId),
                    message_ids: [parseInt(messageId)],
                    revoke: revoke || false
                });
                deletedCount++;

                // Small delay to avoid rate limiting
                await new Promise(resolve => setTimeout(resolve, 50));
            } catch (error) {
                console.log(`Error deleting message ${messageId}:`, error.message);
            }
        }

        res.json({
            success: true,
            deletedCount
        });

    } catch (error) {
        console.error('Delete messages error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Batch scan chats
app.post('/api/batch-scan', async (req, res) => {
    try {
        const { sessionId, chatIds } = req.body;

        if (!sessions.has(sessionId)) {
            return res.status(400).json({
                success: false,
                error: 'Invalid session ID'
            });
        }

        const sessionData = sessions.get(sessionId);

        if (!sessionData.isAuthorized) {
            return res.status(401).json({
                success: false,
                error: 'Not authorized'
            });
        }

        const counts = {};

        for (const chatId of chatIds) {
            try {
                const history = await sessionData.client.invoke({
                    _: 'getChatHistory',
                    chat_id: parseInt(chatId),
                    from_message_id: 0,
                    offset: 0,
                    limit: 1
                });

                // Count messages from the user
                const allHistory = await sessionData.client.invoke({
                    _: 'getChatHistory',
                    chat_id: parseInt(chatId),
                    from_message_id: 0,
                    offset: 0,
                    limit: 100
                });

                counts[chatId] = allHistory.messages.filter(m => m.is_outgoing).length;
            } catch (error) {
                console.log(`Error scanning chat ${chatId}:`, error.message);
                counts[chatId] = 0;
            }
        }

        res.json({
            success: true,
            counts
        });

    } catch (error) {
        console.error('Batch scan error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Logout
app.post('/api/logout', async (req, res) => {
    try {
        const { sessionId } = req.body;

        if (sessions.has(sessionId)) {
            const sessionData = sessions.get(sessionId);

            try {
                await sessionData.client.invoke({ _: 'logOut' });
                await sessionData.client.close();
            } catch (error) {
                console.log('Error during logout:', error.message);
            }

            sessions.delete(sessionId);
        }

        res.json({
            success: true
        });

    } catch (error) {
        console.error('Logout error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

app.listen(PORT, '0.0.0.0', () => {
    const isProduction = process.env.NODE_ENV === 'production';
    const serverUrl = isProduction ? `https://your-domain.com` : `http://localhost:${PORT}`;

    console.log(`
╔═══════════════════════════════════════════════════════╗
║   Telegram Cleaner Backend Server                     ║
║   Running on: ${serverUrl}${' '.repeat(Math.max(0, 29 - serverUrl.length))}║
║                                                        ║
║   Status: ✓ Server is ready                           ║
║   Mode: 🚀 TDLib Integration Active                   ║
║   Environment: ${isProduction ? 'Production' : 'Development'}${' '.repeat(isProduction ? 30 : 27)}║
║                                                        ║
║   Access the app:                                      ║
║   ${isProduction ? 'Visit your domain in a browser' : 'Open http://localhost:' + PORT + ' in a browser'}${' '.repeat(isProduction ? 26 : Math.max(0, 20 - PORT.toString().length))}║
║                                                        ║
║   API Health Check:                                    ║
║   ${serverUrl}/api/health${' '.repeat(Math.max(0, 37 - serverUrl.length))}║
║                                                        ║
║   🔒 Users enter their own API credentials            ║
╚═══════════════════════════════════════════════════════╝
    `);
});

// Handle shutdown gracefully
process.on('SIGINT', async () => {
    console.log('\n\nShutting down server...');

    // Close all TDLib clients
    for (const [sessionId, sessionData] of sessions.entries()) {
        try {
            await sessionData.client.close();
        } catch (error) {
            console.log(`Error closing session ${sessionId}:`, error.message);
        }
    }

    process.exit(0);
});

process.on('SIGTERM', async () => {
    console.log('\n\nShutting down server...');

    // Close all TDLib clients
    for (const [sessionId, sessionData] of sessions.entries()) {
        try {
            await sessionData.client.close();
        } catch (error) {
            console.log(`Error closing session ${sessionId}:`, error.message);
        }
    }

    process.exit(0);
});
