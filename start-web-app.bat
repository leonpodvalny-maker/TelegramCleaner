@echo off
echo ================================================
echo Telegram Cleaner - Web App Version
echo ================================================
echo.

cd /d "%~dp0"

echo Starting backend server...
echo.
echo IMPORTANT: Keep this window open!
echo The server must run for the app to work.
echo.
echo ================================================
echo Server will start on http://localhost:3000
echo ================================================
echo.

REM Start the backend server
node backend-server.js

pause
