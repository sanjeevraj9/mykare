@echo off
REM MyKare Appointment System - Start Backend Only

cd /d d:\Java\Project\mykare\appointement-service

echo.
echo ========================================
echo  MyKare Backend - Starting
echo ========================================
echo.
echo Building and starting Java application...
echo Port: 8087
echo Frontend: http://localhost:8087
echo API Docs: http://localhost:8087/swagger-ui.html
echo.

java -jar target/appointement-service-0.0.1-SNAPSHOT.jar
