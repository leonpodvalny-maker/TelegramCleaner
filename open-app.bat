@echo off
echo ================================================
echo Opening Telegram Cleaner in Browser
echo ================================================
echo.

cd /d "%~dp0"

REM Check if backend server is running
powershell -Command "(Test-NetConnection -ComputerName localhost -Port 3000 -InformationLevel Quiet -WarningAction SilentlyContinue) -eq $true" >nul 2>&1

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Backend server is not running!
    echo.
    echo Please run start-web-app.bat first in a separate window.
    echo.
    pause
    exit /b 1
)

echo Backend server is running!
echo Opening app in default browser...
echo.

start "" "telegram-cleaner-modern.html"

echo.
echo If the browser doesn't open automatically,
echo manually open: telegram-cleaner-modern.html
echo.
pause
