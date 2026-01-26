const { app, BrowserWindow, Menu } = require('electron');
const path = require('path');

let mainWindow;

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1200,
        height: 800,
        minWidth: 800,
        minHeight: 600,
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            enableRemoteModule: false,
            webSecurity: true
        },
        // icon: path.join(__dirname, 'icon.ico'), // Optional: add icon.ico file to project root
        title: 'Telegram Cleaner',
        backgroundColor: '#0E1621'
    });

    // Load the demo/info page first
    mainWindow.loadFile('telegram-cleaner-demo.html');

    // Remove default menu
    Menu.setApplicationMenu(null);

    // Open DevTools in development (optional - remove for production)
    // mainWindow.webContents.openDevTools();

    mainWindow.on('closed', function () {
        mainWindow = null;
    });

    // Handle external links - open in default browser
    mainWindow.webContents.setWindowOpenHandler(({ url }) => {
        require('electron').shell.openExternal(url);
        return { action: 'deny' };
    });
}

// This method will be called when Electron has finished initialization
app.whenReady().then(() => {
    createWindow();

    app.on('activate', function () {
        // On macOS it's common to re-create a window when dock icon is clicked
        if (BrowserWindow.getAllWindows().length === 0) createWindow();
    });
});

// Quit when all windows are closed
app.on('window-all-closed', function () {
    // On macOS apps stay active until user quits explicitly with Cmd + Q
    if (process.platform !== 'darwin') app.quit();
});

// Handle app crashes and errors
process.on('uncaughtException', (error) => {
    console.error('Uncaught Exception:', error);
});

process.on('unhandledRejection', (error) => {
    console.error('Unhandled Rejection:', error);
});
