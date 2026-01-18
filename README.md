# ✨ Telegram Cleaner

A beautiful, modern web application to clean up your Telegram messages with style.

![Telegram Cleaner](https://img.shields.io/badge/Telegram-Cleaner-0088cc?style=for-the-badge&logo=telegram)
![React](https://img.shields.io/badge/React-18.2.0-61dafb?style=for-the-badge&logo=react)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

## 🎯 Features

- **Modern UI/UX** - Beautiful glassmorphism design with smooth animations
- **Smart Search** - Find messages by keywords with highlighted results
- **Bulk Selection** - Select entire chats or individual messages
- **Message Filtering** - Search and filter messages in real-time
- **Responsive Design** - Works seamlessly on desktop and mobile
- **Dark Theme** - Easy on the eyes with a sleek dark interface

## 🚀 Demo Versions

This repository includes three demo versions:

1. **telegram-cleaner.html** - Original version with login flow
2. **telegram-cleaner-modern.html** - Modern UI with glassmorphism effects (recommended)
3. **telegram-cleaner-search.html** - Demonstrates search functionality with highlights

## 📋 Prerequisites

To use this app with real Telegram data, you'll need:

- Telegram API credentials (API ID and API Hash)
- Get them from: [my.telegram.org/apps](https://my.telegram.org/apps)

### How to get API credentials:

1. Visit [my.telegram.org/apps](https://my.telegram.org/apps)
2. Log in with your phone number
3. Fill in the application details (any name works)
4. Copy your `api_id` and `api_hash`
5. Use them in the login form

## 🛠️ Installation

### Quick Start (Demo Mode)

1. Clone the repository:
```bash
git clone https://github.com/leonpodvalny-maker/TelegramCleaner.git
cd TelegramCleaner
```

2. Open any HTML file in your browser:
```bash
# For the modern UI (recommended)
open telegram-cleaner-modern.html

# Or use a local server
python -m http.server 8000
# Then visit: http://localhost:8000/telegram-cleaner-modern.html
```

### For Production Use

**Note:** The current demo uses mock data. To connect to real Telegram API, you'll need to:

1. Set up a backend server (Node.js, Python, etc.)
2. Use Telegram's MTProto API or Bot API
3. Handle authentication securely on the server side

Browser-based apps cannot directly connect to Telegram's API due to security restrictions.

## 🎨 Features Overview

### Login Screen
- Phone number input
- Two-factor authentication (Cloud Password)
- API credentials input with helpful instructions

### Main Dashboard
- **Stats Overview** - Total chats, selected items, messages to delete
- **Search Bar** - Real-time filtering with highlighted results
- **Chat List** - All your conversations with message counts
- **Expandable Messages** - Click any chat to view individual messages
- **Bulk Actions** - Select all, deselect all, delete selected

### Search Results
- **Visual Highlights** - Matching text highlighted in gold
- **Match Badges** - Shows number of matches per chat
- **Smart Filtering** - Filters both chat names and message content
- **Golden Glow** - Chats with matches have a special glow effect

## 🖼️ Screenshots

### Main Dashboard
Beautiful glassmorphism UI with all your chats

### Search Results
Real-time search with highlighted matches

### Message Selection
Expand chats to select individual messages

## 🔧 Technical Details

### Built With
- **React** 18.2.0
- **Babel Standalone** - For JSX transformation
- **CSS3** - Advanced animations and effects
- **Google Fonts** - Outfit & JetBrains Mono

### Key Technologies
- Glassmorphism design
- CSS Grid & Flexbox
- CSS Animations & Transitions
- Gradient backgrounds
- Custom checkboxes
- Smooth scrolling

## ⚠️ Important Notes

### Demo Mode
The current version uses **mock data** for demonstration purposes. It does not:
- Connect to real Telegram servers
- Delete actual messages
- Require real API credentials (in demo mode)

### Privacy & Security
- Never share your API credentials publicly
- Use environment variables for sensitive data
- Always validate user input
- Implement proper authentication for production

### Limitations
- Cannot access groups you've left
- Requires group membership to delete messages
- Rate limits apply to API requests
- Some message types may have deletion restrictions

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📧 Contact

Leon Podvalny - [@leonpodvalny-maker](https://github.com/leonpodvalny-maker)

Project Link: [https://github.com/leonpodvalny-maker/TelegramCleaner](https://github.com/leonpodvalny-maker/TelegramCleaner)

## 🙏 Acknowledgments

- Telegram for their excellent messaging platform
- React team for the amazing library
- Google Fonts for beautiful typography
- The open-source community

---

**⚡ Made with passion by Leon Podvalny**
