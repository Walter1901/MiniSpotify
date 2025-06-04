@echo off
title MiniSpotify Client
cls
echo.
echo ===================================================
echo           🎧 MINI SPOTIFY CLIENT 🎧
echo ===================================================
echo.

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: Java is not installed
    pause
    exit /b 1
)

REM Check JavaFX
if not exist "javafx-lib" (
    echo ❌ ERROR: javafx-lib folder not found
    pause
    exit /b 1
)

REM Check that JAR exists
if not exist "minispotify-client-jar-with-dependencies.jar" (
    echo ❌ ERROR: Client JAR not found
    pause
    exit /b 1
)

echo 🎧 Connecting to server...
echo.

REM Launch client with JavaFX
java --module-path "javafx-lib" ^
     --add-modules javafx.controls,javafx.media,javafx.swing ^
     --add-exports javafx.base/com.sun.javafx.runtime=ALL-UNNAMED ^
     -Dfile.encoding=UTF-8 ^
     -jar minispotify-client-jar-with-dependencies.jar

echo.
echo Client closed.
pause