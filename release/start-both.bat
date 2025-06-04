@echo off
chcp 65001 >nul 2>&1
echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║              🎵 STARTING MINI SPOTIFY COMPLETE 🎵            ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

echo 🚀 Starting server in background...
start "MiniSpotify Server" start-server.bat

echo ⏳ Waiting 3 seconds for server to start...
timeout /t 3 /nobreak >nul

echo 🎮 Starting client...
start "MiniSpotify Client" start-client.bat

echo.
echo ✅ Both server and client started!
echo ℹ️  Close both windows to stop the application.
pause