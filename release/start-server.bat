@echo off
title MiniSpotify Server
cls
echo.
echo ===================================================
echo           🎵 MINI SPOTIFY SERVER 🎵
echo ===================================================
echo.

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: Java is not installed or accessible
    echo Install Java 17 or newer
    pause
    exit /b 1
)

REM Check JavaFX
if not exist "javafx-lib" (
    echo ❌ ERROR: javafx-lib folder not found
    echo Download JavaFX SDK from https://openjfx.io/
    pause
    exit /b 1
)

REM Check that JAR exists
if not exist "minispotify-server.jar" (
    echo ❌ ERROR: minispotify-server.jar not found
    pause
    exit /b 1
)

echo 🚀 Starting server...
echo.

REM Launch server with JavaFX
java --module-path "javafx-lib" ^
     --add-modules javafx.controls,javafx.media,javafx.swing ^
     --add-exports javafx.base/com.sun.javafx.runtime=ALL-UNNAMED ^
     -Dfile.encoding=UTF-8 ^
     -jar minispotify-server.jar

echo.
echo Server stopped.
pause