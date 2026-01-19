# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Telegram Cleaner is a client-side React demo application for managing and deleting Telegram messages. It uses a zero-build architecture with standalone HTML files containing embedded React components, CSS, and JavaScript.

## Running the Application

No build step required. Open any HTML file directly in a browser or use a local server:

```bash
# Recommended: local server
python -m http.server 8000
# Then visit: http://localhost:8000/telegram-cleaner-modern.html

# Alternative: direct file open
open telegram-cleaner-modern.html
```

## Architecture

### Tech Stack
- **React 18.2.0** via CDN (UMD bundle)
- **Babel Standalone 7.23.5** for browser-side JSX transpilation
- **CSS3** with glassmorphism design patterns
- **No package.json, build tools, or dependencies to install**

### File Structure

Each HTML file is a complete, self-contained SPA:

| File | Purpose |
|------|---------|
| `telegram-cleaner-modern.html` | **Recommended** - Modern dashboard with stats, expandable chats, glassmorphism |
| `telegram-cleaner.html` | Original version with login flow |
| `telegram-cleaner-search.html` | Search demo with highlighted results |
| `telegram-cleaner-dashboard.html` | Dashboard/stats focused |
| `telegram-cleaner-messages-expanded.html` | Message expansion demo |

### HTML File Anatomy

Each file contains:
1. `<head>`: Inline CSS (~700+ lines) with glassmorphism design system
2. `<body>`: Single `#root` div for React mounting
3. `<script type="text/babel">`: Complete React application with components and state

### State Management

Uses React hooks with this typical pattern:
```javascript
const [expandedChat, setExpandedChat] = useState(null);
const [selectedChats, setSelectedChats] = useState(new Set());
const [selectedMessages, setSelectedMessages] = useState(new Set());
const [searchQuery, setSearchQuery] = useState('');
```

### Design System (CSS Variables)

- Primary: `#0088cc` (Telegram blue)
- Secondary: `#8e44ad` (Purple accent)
- Dark backgrounds: `#1a1d29`, `#252836`, `#2f3347`
- Fonts: Outfit (UI), JetBrains Mono (numbers)

## Development Workflow

1. Edit HTML file directly
2. Refresh browser to see changes
3. All CSS and JavaScript are inline - no separate files to manage

## Key Limitations

- **Demo mode only**: All data is mock/hardcoded
- **No real Telegram API integration**: Cannot actually connect to Telegram servers
- **No backend**: Client-side only application
- **No testing or linting configured**
