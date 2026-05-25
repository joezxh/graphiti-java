@echo off
echo ===================================
echo OntoGraph Console - 安装依赖
echo ===================================

cd /d %~dp0

echo 正在安装依赖...
npm install

if %ERRORLEVEL% NEQ 0 (
  echo.
  echo [错误] 依赖安装失败，请检查网络连接或手动运行：
  echo   cd /d %~dp0
  echo   npm install
  pause
  exit /b 1
)

echo.
echo ===================================
echo 依赖安装完成！
echo.
echo 启动开发服务器：
echo   npm run dev
echo.
echo 构建生产版本：
echo   npm run build
echo ===================================

pause
