# ✨ Telegram Cleaner

A beautiful, modern web application to clean up your Telegram messages with style.

![Telegram Cleaner](https://img.shields.io/badge/Telegram-Cleaner-0088cc?style=for-the-badge&logo=telegram)
![React](https://img.shields.io/badge/React-18.2.0-61dafb?style=for-the-badge&logo=react)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

## 🎯 Features

- **Modern UI/UX** - Beautiful glassmorphism design with smooth animations
- **Real Telegram API** - Connect with your own API credentials
- **Bulk Deletion** - Delete messages from multiple chats efficiently
- **Message Filtering** - Search and filter messages in real-time
- **Responsive Design** - Works seamlessly on any device
- **Dark Theme** - Easy on the eyes with a sleek dark interface

## 🚀 Quick Start

### Local Testing

1. Clone the repository:
```bash
git clone <your-repo-url>
cd TelegramCleaner
```

2. Install dependencies:
```bash
npm install
```

3. Run the app:
```bash
# Double-click this file (Windows):
RUN-WEB-APP.bat

# Or manually:
node backend-server.js
# Then open http://localhost:3000 in browser
```

4. Get API credentials from [my.telegram.org/apps](https://my.telegram.org/apps)

5. Use the app to manage your messages!

## 🌐 Deploy to Web

Deploy to any platform in minutes:

### Railway (Recommended)
1. Push to GitHub
2. Go to https://railway.app
3. Deploy from GitHub repo
4. Done! Get your public URL

### Heroku
```bash
heroku create your-app
git push heroku main
heroku open
```

See **QUICKSTART.md** for detailed deployment instructions.

## 📋 How It Works

### For Users:
1. Visit your deployed website
2. Get API credentials from https://my.telegram.org/apps
3. Enter your API ID and API Hash
4. Sign in with phone number
5. Manage and delete Telegram messages
6. **No installation required!**

### Architecture:
- **Frontend**: React single-page app (telegram-cleaner-modern.html)
- **Backend**: Node.js + Express + TDLib (backend-server.js)
- **Database**: TDLib manages session data locally

## 🔧 Technical Stack

- **React** 18.2.0 - UI framework
- **TDLib** - Official Telegram client library
- **Express** - Backend API server
- **Node.js** - Server runtime
- **Babel Standalone** - JSX transformation

## 📁 Key Files

- `telegram-cleaner-modern.html` - Main UI
- `backend-server.js` - API server with TDLib
- `RUN-WEB-APP.bat` - Run locally (Windows)
- `Procfile` - Deployment configuration
- `package.json` - Dependencies

## 📚 Documentation

- **START-HERE.md** - Start here!
- **QUICKSTART.md** - Fast deployment guide
- **WEB-DEPLOYMENT-GUIDE.md** - Detailed deployment
- **README-WEB-APP.md** - Full documentation

## ⚠️ Important Notes

### Security
- Each user provides their own API credentials
- Sessions are isolated per user
- Use HTTPS in production (automatic on most platforms)
- Never commit `.tdlib/` directory (contains user data)

### Privacy
- No data is shared between users
- All data stays on your server
- Users control their own credentials

### Limitations
- Telegram API rate limits apply
- Requires Telegram API credentials
- Cannot access groups after leaving

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 🙏 Acknowledgments

- Telegram for their excellent messaging platform and TDLib
- React team for the amazing library
- The open-source community

---

**Made with ❤️ for the Telegram community**
