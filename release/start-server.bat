@echo off
chcp 65001 >nul 2>&1
title MiniSpotify Server
echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║                    🎵 MINI SPOTIFY SERVER 🎵                 ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

REM Check if Java is available
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java not found! Please install Java 11 or higher.
    pause
    exit /b 1
)

echo 🚀 Starting MiniSpotify Server...
echo.

REM JavaFX properties for compatibility
set JAVAFX_OPTS=-Dprism.order=sw -Dprism.allowhidpi=false -Djavafx.animation.fullspeed=true -Dprism.verbose=false -Djava.awt.headless=false

REM Memory settings
set MEMORY_OPTS=-Xms256m -Xmx512m

REM Run the server
java %JAVAFX_OPTS% %MEMORY_OPTS% -jar minispotify-server.jar

echo.
echo 🛑 Server stopped.
pause