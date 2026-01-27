# Quick Start Guide

## Run Locally (Testing)

**Simplest way:**

1. Double-click `RUN-WEB-APP.bat`
2. Wait for browser to open
3. Enter API credentials from https://my.telegram.org/apps
4. Use the app!

## Deploy Online (For Everyone)

### Option 1: Railway (Easiest - 1 minute)

1. **Create GitHub repo** (if you don't have one):
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/yourusername/telegram-cleaner.git
   git push -u origin main
   ```

2. **Deploy to Railway**:
   - Go to https://railway.app
   - Click "Start a New Project"
   - Choose "Deploy from GitHub repo"
   - Select your repo
   - **Done!** Your app is live at: `yourapp.up.railway.app`

### Option 2: Heroku

```bash
heroku create your-app-name
git push heroku main
heroku open
```

### Option 3: Render

1. Go to https://render.com
2. Connect GitHub repo
3. Deploy as "Web Service"
4. Done!

## What Users Need

Users visit your website and:
1. Get API credentials from https://my.telegram.org/apps
2. Enter API ID and API Hash
3. Sign in with phone number
4. Use the app to manage messages

**No installation required for users!**

## Files Needed for Deployment

The essential files are already set up:
- ✅ `backend-server.js` - Server (updated)
- ✅ `telegram-cleaner-modern.html` - UI
- ✅ `package.json` - Dependencies
- ✅ `Procfile` - Deployment config
- ✅ `.gitignore` - Excludes sensitive files

## Test It

1. **Local test**: Run `RUN-WEB-APP.bat`
2. **Deploy**: Push to GitHub → Connect to Railway/Heroku
3. **Share**: Give users your app URL
4. **Done!**

## Troubleshooting

**"Port already in use"**:
```bash
taskkill /F /IM node.exe
```

**"Module not found"**:
```bash
npm install
```

**"TDLib error"**:
- Make sure all dependencies installed: `npm install`
- Check console for detailed error

## More Info

- Full deployment guide: `WEB-DEPLOYMENT-GUIDE.md`
- Debugging help: `DEBUGGING.md`
- Web app README: `README-WEB-APP.md`
