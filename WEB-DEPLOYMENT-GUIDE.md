# Telegram Cleaner - Web Deployment Guide

## Overview

To make this app accessible online, you need to:
1. Host the backend server on a cloud platform
2. Serve the HTML file publicly
3. Users access the website and enter their own API credentials

## Option 1: Simple Deployment (Recommended)

### Services Needed:
- **Backend**: Heroku, Railway, Render, or DigitalOcean
- **Frontend**: GitHub Pages, Netlify, or Vercel (or same server as backend)

### Step-by-Step:

#### 1. Prepare the Backend

The backend (`backend-server.js`) needs to run on a server. You'll need to:

1. **Create a production start script**:
   - Add to `package.json` scripts:
   ```json
   "start-production": "node backend-server.js"
   ```

2. **Set up environment variables** (on your hosting platform):
   ```
   PORT=3000
   NODE_ENV=production
   ```

3. **Deploy backend to hosting service**:
   - **Heroku**: `git push heroku main`
   - **Railway**: Connect GitHub repo
   - **Render**: Connect GitHub repo
   - **DigitalOcean**: Use App Platform

#### 2. Update Frontend to Point to Your Backend

Edit `telegram-cleaner-modern.html` and find the API URL. Change from:
```javascript
const API_URL = 'http://localhost:3000';
```

To your deployed backend URL:
```javascript
const API_URL = 'https://your-app.herokuapp.com'; // or your domain
```

#### 3. Deploy Frontend

**Option A: Same Server as Backend**
- Serve the HTML file from the backend using Express:
  ```javascript
  app.use(express.static(__dirname));
  ```

**Option B: Separate Static Hosting**
- Upload `telegram-cleaner-modern.html` to:
  - GitHub Pages
  - Netlify
  - Vercel
  - Cloudflare Pages

## Option 2: Full Stack Deployment (Single Server)

### Quick Deploy to Railway (Easiest):

1. **Create `Procfile`**:
   ```
   web: node backend-server.js
   ```

2. **Update backend-server.js** to serve the HTML:
   Add this before your API routes:
   ```javascript
   // Serve static files
   app.use(express.static(__dirname));

   // Serve the main HTML file
   app.get('/', (req, res) => {
       res.sendFile(path.join(__dirname, 'telegram-cleaner-modern.html'));
   });
   ```

3. **Push to GitHub**

4. **Deploy on Railway**:
   - Go to railway.app
   - Connect GitHub repo
   - Deploy automatically
   - Get your public URL

## Option 3: Deploy to Heroku

### Steps:

1. **Install Heroku CLI**
   ```bash
   npm install -g heroku
   ```

2. **Login**
   ```bash
   heroku login
   ```

3. **Create app**
   ```bash
   heroku create telegram-cleaner-app
   ```

4. **Update backend-server.js**:
   ```javascript
   const PORT = process.env.PORT || 3000;
   ```

5. **Create Procfile**:
   ```
   web: node backend-server.js
   ```

6. **Deploy**:
   ```bash
   git add .
   git commit -m "Deploy to Heroku"
   git push heroku main
   ```

7. **Open app**:
   ```bash
   heroku open
   ```

## Important Security Considerations

### 1. **CORS Configuration**
Currently the app allows all origins. For production, restrict to your domain:

```javascript
app.use(cors({
    origin: 'https://yourdomain.com',
    credentials: true
}));
```

### 2. **HTTPS Only**
- Telegram requires HTTPS for production
- Most hosting platforms provide free SSL

### 3. **Rate Limiting**
Add rate limiting to prevent abuse:

```bash
npm install express-rate-limit
```

```javascript
const rateLimit = require('express-rate-limit');

const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});

app.use('/api/', limiter);
```

### 4. **User Data**
- Session data is stored in memory (resets on server restart)
- For production, consider using Redis or a database
- TDLib database files are stored in `.tdlib/` directory

## Testing Locally First

Before deploying, test locally:

1. **Start backend**:
   ```bash
   node backend-server.js
   ```

2. **Open in browser**:
   - Open `telegram-cleaner-modern.html` in Chrome/Edge
   - Enter API credentials from https://my.telegram.org
   - Test all features

## User Flow

Once deployed:

1. User visits your website (e.g., `telegram-cleaner.herokuapp.com`)
2. User enters their own Telegram API credentials:
   - API ID
   - API Hash
   - (Get from https://my.telegram.org/apps)
3. User logs in with phone number + verification code
4. User can manage and delete their messages
5. Data is tied to their session (not shared between users)

## Recommended Free Hosting

For testing/small scale:

1. **Railway.app** (Recommended)
   - Easy deployment
   - Free tier: 500 hours/month
   - Automatic HTTPS
   - GitHub integration

2. **Render.com**
   - Free tier available
   - Automatic HTTPS
   - Easy to use

3. **Heroku**
   - Popular platform
   - Free tier (with credit card)
   - Lots of documentation

## Files Needed for Deployment

Minimum files to deploy:
```
/
├── backend-server.js          # Backend API server
├── telegram-cleaner-modern.html  # Frontend UI
├── package.json              # Dependencies
├── Procfile                  # Heroku/Railway startup command
└── .gitignore               # Exclude node_modules, .tdlib, etc.
```

## Next Steps

1. Choose a hosting platform
2. Follow the platform's deployment guide
3. Update the API URL in the HTML file
4. Test with your own Telegram account
5. Share the link with users!

## Need Help?

Common issues:
- **Port already in use**: Change PORT in backend-server.js
- **CORS errors**: Check CORS configuration
- **TDLib not loading**: Ensure prebuilt-tdlib is installed
- **Database errors**: Check .tdlib directory permissions
