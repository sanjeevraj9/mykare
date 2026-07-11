@echo off
REM MyKare Appointment System - Start Python Worker

cd /d d:\Java\Project\mykare\notification-worker

echo.
echo ========================================
echo  MyKare Notification Worker - Starting
echo ========================================
echo.
echo Installing dependencies...
pip install -r requirement.txt

echo.
echo Starting Python worker...
echo Kafka Bootstrap Servers: localhost:9092
echo Topic: appointment-events
echo.

python main.py
