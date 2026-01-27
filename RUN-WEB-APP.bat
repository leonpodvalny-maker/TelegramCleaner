@echo off
echo ================================================
echo Telegram Cleaner - Web App (All-in-One)
echo ================================================
echo.
echo This will:
echo 1. Start the backend server
echo 2. Open the app in your browser
echo.
echo Keep this window OPEN while using the app!
echo ================================================
echo.
pause

cd /d "%~dp0"

echo.
echo [1/2] Starting backend server in background...
start "Telegram Cleaner Backend" /MIN cmd /c "node backend-server.js"

echo [2/2] Waiting for server to start...
timeout /t 3 /nobreak >nul

echo [3/3] Opening app in browser...
start "" "telegram-cleaner-modern.html"

echo.
echo ================================================
echo App is running!
echo ================================================
echo.
echo - The app should open in your browser
echo - A minimized window is running the server
echo - Keep both windows open
echo - Close this window to stop everything
echo.
echo To stop the server:
echo 1. Close this window
echo 2. Close the "Telegram Cleaner Backend" window
echo.
pause

REM When user closes this, kill the background server
taskkill /FI "WINDOWTITLE eq Telegram Cleaner Backend*" /F >nul 2>&1
