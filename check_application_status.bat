@echo off
echo Checking Trading Bot Application Status...
echo.

:loop
echo [%date% %time%] Checking Java processes...
tasklist /FI "IMAGENAME eq java.exe" 2>NUL | find /I /N "java.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo ✅ Trading Bot Application is RUNNING
    echo.
    tasklist /FI "IMAGENAME eq java.exe" /FO TABLE
    echo.
    echo Application will continue running...
    echo Press Ctrl+C to stop this monitoring script
    echo.
) else (
    echo ❌ Trading Bot Application is NOT RUNNING
    echo.
    echo Starting the application...
    java -jar target/tradingbot-0.0.1-SNAPSHOT.jar
)

timeout /t 60 /nobreak > nul
goto loop
