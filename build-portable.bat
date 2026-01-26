@echo off
echo ========================================
echo Telegram Cleaner - Portable EXE Builder
echo ========================================
echo.

REM Check if Node.js is installed
where npm >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js is not installed!
    echo.
    echo Please download and install Node.js from:
    echo https://nodejs.org/
    echo.
    pause
    exit /b 1
)

echo [1/4] Checking package.json...
if not exist package.json (
    if exist electron-package.json (
        echo Found electron-package.json, renaming to package.json...
        copy electron-package.json package.json
    ) else (
        echo [ERROR] package.json not found!
        pause
        exit /b 1
    )
)

echo [2/4] Installing dependencies...
echo This may take a few minutes...
call npm install
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to install dependencies!
    pause
    exit /b 1
)

echo.
echo [3/4] Building portable executable...
echo This may take 3-5 minutes...
call npm run build
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b 1
)

echo.
echo [4/4] Build complete!
echo.
echo ========================================
echo SUCCESS!
echo ========================================
echo.
echo Your portable executable is ready at:
echo.
echo   dist\TelegramCleaner-Portable.exe
echo.
echo You can copy this file to any Windows PC and run it without installation.
echo.
echo File size: ~150-180 MB (includes everything needed)
echo.

REM Open the dist folder
if exist dist\TelegramCleaner-Portable.exe (
    echo Opening dist folder...
    explorer dist
)

pause
