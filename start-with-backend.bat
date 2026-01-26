@echo off
echo ========================================
echo Telegram Cleaner - Starting...
echo ========================================
echo.

REM Check if Node.js is installed
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js is not installed!
    echo.
    echo Please download and install Node.js from:
    echo https://nodejs.org/
    echo.
    pause
    exit /b 1
)

REM Check if dependencies are installed
if not exist "node_modules\express" (
    echo [1/3] Installing dependencies...
    call npm install
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Failed to install dependencies!
        pause
        exit /b 1
    )
    echo.
)

echo [2/3] Starting backend server...
echo.
start "Telegram Cleaner Backend" cmd /k "node backend-server.js"

echo Waiting for backend to start...
timeout /t 3 >nul

echo [3/3] Starting Telegram Cleaner app...
echo.
start "" "dist\win-unpacked\Telegram Cleaner.exe"

echo.
echo ========================================
echo SUCCESS!
echo ========================================
echo.
echo Backend server is running in a separate window.
echo The Telegram Cleaner app should open shortly.
echo.
echo IMPORTANT: Keep the backend window open!
echo Close it only when you're done using the app.
echo.
pause
