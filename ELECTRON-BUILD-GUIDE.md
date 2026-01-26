# Building Telegram Cleaner Portable Windows Executable

This guide explains how to build a portable Windows .exe file that requires no installation.

## Prerequisites

1. **Node.js** (v16 or higher) - Download from https://nodejs.org/
2. **Git** (optional, for cloning)

## Quick Build Instructions

### Step 1: Install Dependencies

Open a terminal in the project folder and run:

```bash
npm install --save-dev electron@28.1.0 electron-builder@24.9.1
```

This will create a `node_modules` folder and install Electron.

### Step 2: Rename package.json

```bash
# Backup existing package.json if it exists
move package.json package.json.backup

# Use the Electron package.json
move electron-package.json package.json
```

Or simply rename `electron-package.json` to `package.json` manually in File Explorer.

### Step 3: Add an Icon (Optional)

- Create or download a 256x256 PNG icon for your app
- Convert it to .ico format using an online tool: https://convertico.com/
- Save it as `icon.ico` in the project root
- If you skip this step, Windows will use a default icon

### Step 4: Build the Portable Executable

```bash
npm run build
```

This will:
- Package your app with Electron
- Create a portable .exe file in the `dist` folder
- Takes 2-5 minutes depending on your computer

### Step 5: Get Your Portable App

After the build completes, find your portable executable at:

```
dist/TelegramCleaner-Portable.exe
```

This single .exe file can be:
- Copied to any Windows computer
- Run without installation
- Run from a USB drive
- Shared with others (no dependencies needed!)

## Running the App in Development Mode

To test the app before building:

```bash
npm start
```

This opens the app in development mode with live reload.

## Troubleshooting

### "npm is not recognized"
- Node.js is not installed or not in your PATH
- Restart your terminal after installing Node.js
- Or download Node.js from https://nodejs.org/

### Build fails with "Cannot find module"
- Run `npm install` again
- Delete `node_modules` folder and run `npm install` fresh

### Build fails with "Cannot create symbolic link" error
**THIS IS THE MOST COMMON ISSUE ON WINDOWS!**

Error message looks like:
```
ERROR: Cannot create symbolic link : A required privilege is not held by the client
```

**Solutions (choose one):**

1. **Run as Administrator** (easiest):
   - Right-click `build-portable.bat`
   - Select "Run as Administrator"
   - Build will complete successfully

2. **Enable Developer Mode** (Windows 10/11):
   - Open Settings → Update & Security → For Developers
   - Turn on "Developer Mode"
   - Run `build-portable.bat` normally

3. **Use the unpacked version**:
   - The app is already built in `dist/win-unpacked/`
   - Run `dist/win-unpacked/telegram-cleaner.exe`
   - Works perfectly, just not a single .exe file
   - You can zip this folder and distribute it

4. **Disable Windows Defender/Antivirus temporarily**:
   - Sometimes antivirus blocks symlink creation
   - Disable temporarily and try building again

### Portable exe is too large (>150MB)
- This is normal for Electron apps
- The exe includes Chrome browser engine (~100MB)
- Can't be reduced significantly without removing features

### Icon doesn't appear
- Make sure `icon.ico` exists in the project root
- Icon must be in .ico format (not .png or .jpg)
- Rebuild after adding the icon

### Build gets stuck at "packaging" stage
- Be patient - first build takes 5-10 minutes
- Downloads 100+ MB of Electron
- Subsequent builds are much faster (2-3 minutes)

## Build Options

### Regular Installer (instead of portable)
```bash
npm run build-installer
```

This creates a traditional Windows installer in `dist/` folder.

### Build for 32-bit Windows
Edit `electron-package.json` and change:
```json
"arch": ["x64"]
```
to:
```json
"arch": ["ia32"]
```

### Build both 32-bit and 64-bit
```json
"arch": ["x64", "ia32"]
```

## File Sizes

- **Source HTML**: ~1 MB
- **Portable .exe**: ~150-180 MB (includes Chromium engine)
- **With installer**: ~150 MB installer + ~200 MB installed

## Distribution

The portable .exe file:
- Contains everything needed to run
- No installation required
- No dependencies needed
- Can be distributed as a single file
- Works on Windows 7, 8, 10, and 11

## Advanced Configuration

Edit `electron-package.json` to customize:
- App name and description
- Window size and behavior
- Build output location
- Compression settings
- Auto-update features

See https://www.electron.build/ for full documentation.
