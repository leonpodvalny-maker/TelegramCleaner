@echo off
echo ========================================
echo Telegram Cleaner - Quick Launch
echo ========================================
echo.

if not exist "dist\win-unpacked\Telegram Cleaner.exe" (
    echo [ERROR] App not built yet!
    echo.
    echo Please run build-portable.bat first
    pause
    exit /b 1
)

echo Launching Telegram Cleaner...
echo.
start "" "dist\win-unpacked\Telegram Cleaner.exe"

echo.
echo App launched! You can close this window.
echo.
timeout /t 3 >nul
