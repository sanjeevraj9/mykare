#!/bin/bash
# MyKare Appointment System - Start All Services (Linux/Mac)

echo ""
echo "========================================"
echo "  MyKare Appointment System Startup"
echo "========================================"
echo ""
echo "Starting services..."
echo ""

# Start Java Backend in background
echo "[1/2] Starting Java Backend (Port 8087)..."
cd "$(dirname "$0")/appointement-service"
java -jar target/appointement-service-0.0.1-SNAPSHOT.jar &
BACKEND_PID=$!

sleep 8

# Start Python Worker in background
echo "[2/2] Starting Python Worker (Kafka Consumer)..."
cd "$(dirname "$0")/notification-worker"
python main.py &
WORKER_PID=$!

sleep 3

echo ""
echo "========================================"
echo "  Services Started Successfully!"
echo "========================================"
echo ""
echo "Backend:  http://localhost:8087"
echo "API Docs: http://localhost:8087/swagger-ui.html"
echo "Database: http://localhost:8087/h2-console"
echo ""
echo "Process IDs:"
echo "  Backend: $BACKEND_PID"
echo "  Worker:  $WORKER_PID"
echo ""
echo "To stop services, run:"
echo "  kill $BACKEND_PID $WORKER_PID"
echo ""

# Keep script running
wait
