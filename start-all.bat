@echo off
REM MyKare Appointment System - Start All Services
REM This script starts the backend and Python worker

cls
echo.
echo ========================================
echo  MyKare Appointment System Startup
echo ========================================
echo.
echo Starting services...
echo.

REM Start Java Backend in new window
echo [1/2] Starting Java Backend (Port 8087)...
start "MyKare Backend" cmd /k "cd /d d:\Java\Project\mykare\appointement-service && java -jar target/appointement-service-0.0.1-SNAPSHOT.jar"

timeout /t 8 /nobreak

REM Start Python Worker in new window
echo [2/2] Starting Python Worker (Kafka Consumer)...
start "MyKare Python Worker" cmd /k "cd /d d:\Java\Project\mykare\notification-worker && python main.py"

timeout /t 3 /nobreak

echo.
echo ========================================
echo  Services Starting...
echo ========================================
echo.
echo Backend:  http://localhost:8087
echo API Docs: http://localhost:8087/swagger-ui.html
echo Database: http://localhost:8087/h2-console
echo.
echo Note: This window will close in 5 seconds...
timeout /t 5 /nobreak
exit
