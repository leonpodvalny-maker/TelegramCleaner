# Telegram Cleaner - Web App

A web-based application for managing and cleaning your Telegram messages.

## Quick Start (Local Testing)

### 1. Install Dependencies
```bash
npm install
```

### 2. Run the App
```bash
# Option A: Double-click this file
RUN-WEB-APP.bat

# Option B: Manual start
node backend-server.js
# Then open http://localhost:3000 in your browser
```

### 3. Use the App
1. Get API credentials from https://my.telegram.org/apps
2. Enter your API ID and API Hash
3. Sign in with your phone number
4. Manage your Telegram messages

## Deploy to the Web

See `WEB-DEPLOYMENT-GUIDE.md` for full deployment instructions.

### Quick Deploy to Railway:

1. **Push to GitHub**:
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git push origin main
   ```

2. **Deploy on Railway**:
   - Go to https://railway.app
   - Sign in with GitHub
   - Click "New Project" → "Deploy from GitHub repo"
   - Select your repo
   - Railway will auto-detect and deploy!

3. **Done!**
   - Your app is now live at: `your-app.up.railway.app`
   - Share the link with users

### Quick Deploy to Heroku:

```bash
# Install Heroku CLI first
heroku login
heroku create your-app-name
git push heroku main
heroku open
```

## How It Works

### Architecture:
- **Frontend**: `telegram-cleaner-modern.html` (React single-page app)
- **Backend**: `backend-server.js` (Express + TDLib)
- **Database**: TDLib stores session data in `.tdlib/` directory

### User Flow:
1. User visits your website
2. Enters their own Telegram API credentials
3. Authenticates with phone number
4. Manages messages through the UI
5. All data is tied to their session

### Security:
- Each user provides their own API credentials
- Sessions are isolated
- No shared data between users
- HTTPS recommended for production

## Files

- `backend-server.js` - Express API server with TDLib integration
- `telegram-cleaner-modern.html` - React frontend UI
- `package.json` - Dependencies and scripts
- `Procfile` - Heroku/Railway deployment configuration
- `.gitignore` - Excludes node_modules and user data

## Requirements

- Node.js v16 or higher
- npm
- Internet connection (for Telegram API)

## API Endpoints

- `GET /` - Serves the web app
- `GET /api/health` - Health check
- `POST /api/init` - Initialize Telegram session
- `POST /api/send-code` - Send verification code
- `POST /api/sign-in` - Sign in with code
- `POST /api/sign-in-2fa` - Sign in with 2FA password
- `GET /api/dialogs` - Get chat list
- `GET /api/messages` - Get messages from a chat
- `POST /api/delete-messages` - Delete messages
- `POST /api/batch-scan` - Scan multiple chats
- `POST /api/logout` - Logout

## Environment Variables

- `PORT` - Server port (default: 3000)
- `NODE_ENV` - Environment (development/production)

## Development

```bash
# Install dependencies
npm install

# Run in development
npm run dev
# OR
node backend-server.js

# Open in browser
http://localhost:3000
```

## Production Deployment

See `WEB-DEPLOYMENT-GUIDE.md` for detailed instructions on deploying to:
- Railway
- Heroku
- Render
- DigitalOcean
- Vercel + separate backend

## Troubleshooting

### "Cannot find module 'tdl'"
```bash
npm install
```

### "Port 3000 already in use"
```bash
# Change PORT in backend-server.js or set environment variable
export PORT=3001  # Linux/Mac
set PORT=3001     # Windows
```

### "TDLib not found"
The `prebuilt-tdlib` package should be installed automatically.
If not:
```bash
npm install prebuilt-tdlib
```

### API Errors
- Check console logs with: `node backend-server.js`
- Verify API credentials are correct
- Ensure internet connection is stable

## Support

For issues or questions:
1. Check `WEB-DEPLOYMENT-GUIDE.md`
2. Check `DEBUGGING.md`
3. Review console logs for errors

## License

MIT
