@echo off
echo ========================================
echo     Trading Bot Log Monitor
echo ========================================
echo.

REM Check if logs directory exists
if not exist "logs" (
    echo Creating logs directory...
    mkdir logs
    echo.
)

REM Check if log file exists
if not exist "logs\tradingbot.log" (
    echo No log file found. The application might not have started yet.
    echo Please start the application first using: java -jar target\tradingbot-0.0.1-SNAPSHOT.jar
    pause
    exit /b 1
)

echo Available log monitoring options:
echo.
echo 1. View recent logs (last 50 lines)
echo 2. Monitor logs in real-time (Ctrl+C to stop)
echo 3. Search for errors
echo 4. Search for signals
echo 5. Search for specific text
echo 6. View log file size and info
echo 7. Exit
echo.

set /p choice="Enter your choice (1-7): "

if "%choice%"=="1" (
    echo.
    echo === RECENT LOGS (Last 50 lines) ===
    powershell "Get-Content logs\tradingbot.log -Tail 50"
    echo.
    pause
    goto :eof
)

if "%choice%"=="2" (
    echo.
    echo === REAL-TIME LOG MONITORING ===
    echo Press Ctrl+C to stop monitoring...
    echo.
    powershell "Get-Content logs\tradingbot.log -Wait -Tail 10"
    goto :eof
)

if "%choice%"=="3" (
    echo.
    echo === ERROR SEARCH ===
    powershell "Select-String -Path logs\tradingbot.log -Pattern 'ERROR|Exception|Failed|❌' -Context 2"
    echo.
    pause
    goto :eof
)

if "%choice%"=="4" (
    echo.
    echo === SIGNAL SEARCH ===
    powershell "Select-String -Path logs\tradingbot.log -Pattern 'SIGNAL|Strategy.*EXECUTION|position opened' -Context 1"
    echo.
    pause
    goto :eof
)

if "%choice%"=="5" (
    set /p searchtext="Enter text to search for: "
    echo.
    echo === SEARCH RESULTS for "%searchtext%" ===
    powershell "Select-String -Path logs\tradingbot.log -Pattern '%searchtext%' -Context 1"
    echo.
    pause
    goto :eof
)

if "%choice%"=="6" (
    echo.
    echo === LOG FILE INFO ===
    powershell "Get-ChildItem logs\tradingbot.log | Select-Object Name, Length, LastWriteTime"
    echo.
    echo === LOG FILE STATISTICS ===
    powershell "$content = Get-Content logs\tradingbot.log; Write-Host 'Total Lines:' $content.Count; Write-Host 'Error Count:' ($content | Select-String -Pattern 'ERROR|Exception').Count; Write-Host 'Warning Count:' ($content | Select-String -Pattern 'WARN').Count; Write-Host 'Info Count:' ($content | Select-String -Pattern 'INFO').Count"
    echo.
    pause
    goto :eof
)

if "%choice%"=="7" (
    echo Goodbye!
    exit /b 0
)

echo Invalid choice. Please try again.
pause
goto :eof
