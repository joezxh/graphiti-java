@echo off
cd /d D:\projects\ontograph-java\graphiti-web
echo Installing dependencies...
npm install
echo.
echo Installation complete!
echo Starting dev server...
npm run dev
pause
