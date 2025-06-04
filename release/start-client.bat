@echo off
chcp 65001 >nul 2>&1
title MiniSpotify Client
echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║                    🎧 MINI SPOTIFY CLIENT 🎧                 ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

REM Check if Java is available
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java not found! Please install Java 11 or higher.
    pause
    exit /b 1
)

echo 🎮 Starting MiniSpotify Client...
echo 💡 Make sure the server is running first!
echo.

REM JavaFX properties - CRITICAL for audio playback
set JAVAFX_OPTS=-Dprism.order=sw -Dprism.allowhidpi=false -Djavafx.animation.fullspeed=true -Dprism.verbose=false -Djava.awt.headless=false -Dprism.forceGPU=false

REM Memory settings
set MEMORY_OPTS=-Xms128m -Xmx256m

REM Run the client
java %JAVAFX_OPTS% %MEMORY_OPTS% -jar minispotify-client-jar-with-dependencies.jar

echo.
echo 👋 Client closed.
pause