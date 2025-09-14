@echo off
echo Starting Trading Bot Application...
echo.

REM Check if JAR file exists
if not exist "target\tradingbot-0.0.1-SNAPSHOT.jar" (
    echo ❌ JAR file not found. Building application...
    echo.
    call mvn clean package -DskipTests
    echo.
    if not exist "target\tradingbot-0.0.1-SNAPSHOT.jar" (
        echo ❌ Build failed. Please check the errors above.
        pause
        exit /b 1
    )
)

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java is not installed or not in PATH
    pause
    exit /b 1
)

echo ✅ Starting Trading Bot Application...
echo.
echo The application will run continuously.
echo Check your Telegram for notifications.
echo Press Ctrl+C to stop the application.
echo.

java -jar target/tradingbot-0.0.1-SNAPSHOT.jar

echo.
echo Application stopped.
pause
