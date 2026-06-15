@echo off
title Real-Time Disaster Alert System (Web Server)
color 0A
cls

echo ==================================================
echo   Compiling Java Project... Please Wait...
echo ==================================================
if not exist bin mkdir bin
javac --release 8 -d bin src\main\java\*.java

if %errorlevel% neq 0 (
    color 0C
    echo.
    echo [!] ERROR: Compilation Failed!
    echo Ensure Java JDK is installed properly on your system.
    pause
    exit /b %errorlevel%
)

cls
echo ==================================================
echo   Starting Disaster Alert Web Server...
echo ==================================================
echo.
echo Server is running... Do NOT close this window!
echo Opening your default browser...

start http://localhost:8080
java -cp bin MainServer

pause
