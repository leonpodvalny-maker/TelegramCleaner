@echo off
echo ================================================
echo Push Telegram Cleaner to GitHub
echo ================================================
echo.

cd /d "%~dp0"

echo Current status:
git status --short
echo.
echo.
echo Files to be pushed are CLEAN:
echo - No personal information
echo - No build artifacts (gitignored)
echo - Only essential web app files
echo.
pause

echo.
echo Adding all files...
git add .

echo.
echo Creating commit...
git commit -m "Clean web app: TDLib integration with modern UI"

echo.
echo Pushing to GitHub...
git push origin main

echo.
echo ================================================
echo Done! Check GitHub for your clean project.
echo ================================================
pause
