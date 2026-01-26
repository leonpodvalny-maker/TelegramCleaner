/**
 * Simple Backend Server for Telegram Cleaner Desktop App
 *
 * This server provides API endpoints that the Electron app connects to.
 * It handles communication with Telegram's TDLib.
 *
 * Run with: node backend-server.js
 */

const express = require('express');
const cors = require('cors');
const app = express();
const PORT = 3000;

app.use(cors());
app.use(express.json());

// Session storage (in-memory for demo)
const sessions = new Map();

// Health check endpoint
app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', message: 'Backend server is running' });
});

// Initialize Telegram session
app.post('/api/init', async (req, res) => {
    try {
        const { apiId, apiHash, sessionString } = req.body;

        // TODO: Initialize TDLib here
        // For now, return mock response
        const sessionId = Date.now().toString();

        sessions.set(sessionId, {
            apiId,
            apiHash,
            sessionString,
            isAuthorized: false
        });

        res.json({
            success: true,
            sessionId,
            isAuthorized: false,
            user: null
        });
    } catch (error) {
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

        // TODO: Send code via TDLib

        res.json({
            success: true,
            message: 'Code sent to phone'
        });
    } catch (error) {
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

        // TODO: Verify code with TDLib

        res.json({
            success: true,
            user: {
                id: '12345',
                firstName: 'Demo',
                lastName: 'User',
                phone: '+1234567890'
            }
        });
    } catch (error) {
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

        // TODO: Get actual dialogs from TDLib
        // Return mock data for now
        res.json({
            success: true,
            dialogs: [
                {
                    id: '1',
                    name: 'John Doe',
                    type: 'private',
                    lastMessage: 'Hey, how are you?',
                    unreadCount: 2,
                    messageCount: 150
                },
                {
                    id: '2',
                    name: 'Work Group',
                    type: 'group',
                    lastMessage: 'Meeting at 3pm',
                    unreadCount: 5,
                    messageCount: 500
                }
            ]
        });
    } catch (error) {
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

        // TODO: Get actual messages from TDLib
        // Return mock data for now
        res.json({
            success: true,
            messages: [
                {
                    id: '101',
                    chatId,
                    content: 'Hello!',
                    date: Date.now() - 3600000,
                    isOutgoing: true
                },
                {
                    id: '102',
                    chatId,
                    content: 'How are you?',
                    date: Date.now() - 3500000,
                    isOutgoing: true
                }
            ]
        });
    } catch (error) {
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

        // TODO: Delete messages via TDLib

        res.json({
            success: true,
            deletedCount: messageIds.length
        });
    } catch (error) {
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

        // TODO: Scan chats via TDLib
        // Return mock counts
        const counts = {};
        chatIds.forEach(id => {
            counts[id] = Math.floor(Math.random() * 100);
        });

        res.json({
            success: true,
            counts
        });
    } catch (error) {
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

        sessions.delete(sessionId);

        res.json({
            success: true
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

app.listen(PORT, () => {
    console.log(`
╔═══════════════════════════════════════════════════════╗
║   Telegram Cleaner Backend Server                     ║
║   Running on: http://localhost:${PORT}                    ║
║                                                        ║
║   Status: ✓ Server is ready                           ║
║   Mode: Demo (Mock Data)                               ║
║                                                        ║
║   Next Steps:                                          ║
║   1. Keep this terminal open                           ║
║   2. Run the Electron app                              ║
║   3. The app will connect to this server               ║
║                                                        ║
║   Note: This is using mock data. To connect to real   ║
║   Telegram, you need to integrate TDLib.               ║
╚═══════════════════════════════════════════════════════╝
    `);
});

// Handle shutdown gracefully
process.on('SIGINT', () => {
    console.log('\n\nShutting down server...');
    process.exit(0);
});

process.on('SIGTERM', () => {
    console.log('\n\nShutting down server...');
    process.exit(0);
});
