@echo off
rem Smart Fire and Rescue Response System - Windows CMD Runner
cd /d "%~dp0"

echo ==========================================================
echo  Starting Smart Fire ^& Rescue Response System...
echo ==========================================================

rem Free port 8080 if an older server instance is already running
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080" ^| findstr "LISTENING"') do (
    taskkill /f /pid %%a >nul 2>&1
)

if not exist out mkdir out
echo [BUILD] Compiling Java source files...
javac -encoding UTF-8 -cp "lib\mysql-connector-j.jar" -d out src\*.java
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)

echo [RUN] Launching server on http://localhost:8080 ...
java -cp "out;lib\mysql-connector-j.jar" Main
pause
