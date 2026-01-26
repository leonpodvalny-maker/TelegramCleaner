# Telegram Cleaner Desktop App - Setup Guide

## ⚠️ Important: Backend Server Required

The "Failed to fetch" error occurs because the desktop app requires a backend server to communicate with Telegram's API.

## Quick Solution - Run With Backend

I've created a complete solution for you:

### Option 1: Automatic Start (Recommended)

Double-click: **`start-with-backend.bat`**

This will:
1. Start the backend server (in a separate window)
2. Launch the Telegram Cleaner app
3. Everything connects automatically!

**Important:** Keep the backend window open while using the app!

### Option 2: Manual Start

**Terminal 1 - Start Backend:**
```bash
npm run server
```
Keep this running!

**Terminal 2 - Start App:**
```bash
npm start
```
Or run `dist\win-unpacked\Telegram Cleaner.exe`

## What I Created

1. **Backend Server** (`backend-server.js`)
   - Provides API endpoints for the Electron app
   - Currently returns mock/demo data
   - Ready for real Telegram API integration

2. **Demo Page** (`telegram-cleaner-demo.html`)
   - Shows if backend is not running
   - Helpful error messages
   - Instructions for setup

3. **Startup Script** (`start-with-backend.bat`)
   - Starts everything automatically
   - Easiest way to run the app

## Current Status

✅ **Backend Server:** Created and ready (mock data mode)
✅ **Electron App:** Built and configured
✅ **Startup Scripts:** Ready to use
⚠️ **Real Telegram API:** Not yet integrated (needs TDLib)

## How It Works

```
[Telegram Cleaner.exe]
         ↓
    (HTTP requests)
         ↓
[Backend Server on localhost:3000]
         ↓
    (Would connect to)
         ↓
[Telegram API via TDLib]
```

## For Real Telegram Integration

To connect to real Telegram instead of mock data:

1. **Get Telegram API Credentials:**
   - Visit: https://my.telegram.org/apps
   - Create an application
   - Note your `api_id` and `api_hash`

2. **Install TDLib:**
   - The backend needs TDLib (Telegram Database Library)
   - See: https://github.com/tdlib/td

3. **Update Backend:**
   - Modify `backend-server.js`
   - Replace mock responses with real TDLib calls
   - Similar to how the Android version works

## Alternative: Use Android Version

The Android version (`android/` folder) has full Telegram integration:
- Native TDLib integration
- No backend server needed
- Complete Telegram API access

To build the Android APK:
```bash
npx cap sync android
cd android
./gradlew assembleDebug
```

## Troubleshooting

### "Failed to fetch" error
- Backend server is not running
- Run `start-with-backend.bat` instead

### "Port 3000 already in use"
- Another app is using port 3000
- Stop the other app or change port in `backend-server.js`

### App shows demo page
- Backend is not running or not accessible
- Check firewall settings
- Try `http://localhost:3000/api/health` in browser

### Backend window closes immediately
- Dependencies not installed
- Run: `npm install`

## Demo vs Production

**Current Setup (Demo Mode):**
- Backend returns mock data
- No real Telegram connection
- Good for testing UI/UX
- Safe to use without API credentials

**Production Mode (Not Yet Implemented):**
- Would need real Telegram API credentials
- TDLib integration required
- Full access to your Telegram account
- Can actually delete messages

## Files Overview

```
TelegramCleaner/
├── backend-server.js              ← Backend API server
├── telegram-cleaner-demo.html     ← Error/info page
├── telegram-cleaner-modern.html   ← Main app UI
├── electron-main.js               ← Electron wrapper
├── start-with-backend.bat         ← Easy startup script
├── dist/
│   └── win-unpacked/
│       └── Telegram Cleaner.exe   ← Desktop app
└── package.json                   ← Dependencies & scripts
```

## Development Commands

```bash
# Start backend server only
npm run server

# Start Electron app only
npm start

# Start both together
npm run dev

# Build portable exe
npm run build
```

## Next Steps

1. **Test the app:**
   ```bash
   start-with-backend.bat
   ```

2. **Explore the UI:**
   - App will show mock data
   - Try the interface
   - See how everything works

3. **For real integration:**
   - Either use the Android version
   - Or integrate TDLib into backend-server.js

## Questions?

- **Backend errors?** Check the backend terminal window
- **App won't connect?** Make sure backend is running
- **Want real Telegram?** Consider using Android version
- **Build issues?** See `PORTABLE-EXE-GUIDE.md`

---

**Quick Start:** Just run `start-with-backend.bat` and you're good to go! 🚀
