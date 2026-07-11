# MyKare Appointment System - Complete Setup Guide

## 📋 Project Overview
MyKare is a microservices-based appointment booking system with:
- **Backend**: Spring Boot REST API (Java) - Port 8087
- **Frontend**: Web UI (HTML/CSS/JS) - Integrated with backend
- **Message Queue**: Kafka - For event-driven notifications
- **Worker**: Python notification processor - Listens to appointment events

---

## 🚀 Quick Start (All-in-One)

### Option 1: Run All Services (Recommended)

**Windows - Run `start-all.bat`**:
```batch
# This will start:
# 1. Kafka (if available)
# 2. Java Backend (Port 8087)
# 3. Python Worker
```

**Manual - Step by Step**:

#### Step 1: Start Kafka (if available)
```bash
cd d:\Java\Project\mykare
docker-compose up -d kafka
# or if Kafka is already running, skip this
```

#### Step 2: Start Java Backend
```bash
cd d:\Java\Project\mykare\appointement-service
java -jar target/appointement-service-0.0.1-SNAPSHOT.jar
```

#### Step 3: Start Python Worker (in another terminal)
```bash
cd d:\Java\Project\mykare\notification-worker
python -m pip install -r requirement.txt
python main.py
```

#### Step 4: Access the Application
- **Frontend**: http://localhost:8087
- **API Docs**: http://localhost:8087/swagger-ui.html
- **Database Console**: http://localhost:8087/h2-console

---

## 📝 Service Details

### 1. Backend - Spring Boot (Java)

**Port**: 8087  
**Location**: `appointement-service/`

**Key Endpoints**:
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token
- `POST /api/appointments` - Book appointment
- `GET /api/appointments` - List appointments
- `GET /api/appointments/available-slots` - Get available slots
- `PATCH /api/appointments/{id}/cancel` - Cancel appointment

**Technologies**:
- Spring Boot 4.1.0
- Hibernate 7.4.1
- H2 Database (in-memory for dev)
- Spring Security + JWT
- Spring Kafka

**Configuration** (`src/main/resources/application.yml`):
```yaml
server:
  port: 8087
spring:
  datasource:
    url: jdbc:h2:mem:mykare;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
  kafka:
    bootstrap-servers: localhost:9092
```

---

### 2. Frontend - Web UI (HTML/CSS/JS)

**Location**: `appointement-service/src/main/resources/static/index.html`

**Features**:
- User registration and login
- Appointment booking with date/time picker
- Available slots viewer
- Appointment history and cancellation
- Real-time status updates
- Beautiful modern UI with animations

**API Integration**:
- All requests use JWT token from login
- Automatic error handling and user feedback
- Color-coded status messages

---

### 3. Message Queue - Apache Kafka

**Port**: 9092  
**Topic**: `appointment-events`

**Flow**:
1. Java backend publishes appointment events to Kafka
2. Python worker listens to events
3. Worker processes notifications
4. Worker updates appointment status back to backend

**Event Structure**:
```json
{
  "appointmentId": 1,
  "patientName": "John Doe",
  "patientEmail": "john@example.com",
  "doctorName": "Dr. Smith",
  "appointmentTime": "2026-07-10T14:00:00",
  "status": "BOOKED",
  "eventType": "APPOINTMENT_BOOKED"
}
```

---

### 4. Worker - Python Notification Processor

**Location**: `notification-worker/`  
**Language**: Python 3.8+

**Dependencies**:
- `kafka-python==2.0.2` - Kafka consumer
- `requests==2.32.3` - HTTP client

**Flow**:
1. Connects to Kafka broker
2. Listens for appointment events
3. Processes notifications (currently logs them)
4. Updates appointment notification status via REST API

**Configuration** (via environment variables):
```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
APPOINTMENT_TOPIC=appointment-events
KAFKA_CONSUMER_GROUP=notification-worker
APPOINTMENT_SERVICE_URL=http://localhost:8087
LOG_LEVEL=INFO
```

**To Run**:
```bash
cd notification-worker
pip install -r requirement.txt
python main.py
```

---

## 🔄 How It Works - End-to-End Flow

### User Journey:

```
1. USER OPENS APP
   └─> http://localhost:8087
   └─> UI loads from backend

2. USER REGISTERS
   └─> Clicks "Register"
   └─> POST /api/auth/register
   └─> Backend creates user in H2 database
   └─> Returns JWT token
   └─> Frontend stores token for future requests

3. USER BOOKS APPOINTMENT
   └─> Fills form (patient name, doctor, time, reason)
   └─> Clicks "Book Now"
   └─> POST /api/appointments (with JWT token)
   └─> Backend:
       ├─> Validates inputs
       ├─> Checks if slot is available (race condition prevention)
       ├─> Saves appointment to H2 database
       ├─> Publishes event to Kafka topic
       └─> Returns confirmation

4. PYTHON WORKER PROCESSES EVENT
   └─> Listens to Kafka topic
   └─> Receives APPOINTMENT_BOOKED event
   └─> Updates status to "PROCESSING"
   └─> Logs notification (stub - can integrate SMS/Email/WhatsApp)
   └─> Updates status to "SENT"
   └─> Frontend can view history with status updates

5. USER VIEWS APPOINTMENTS
   └─> GET /api/appointments
   └─> Displays all bookings with latest status
   └─> Can cancel, view history, or check available slots
```

---

## 🛠️ Development & Troubleshooting

### Build the Project
```bash
cd appointement-service
mvn clean package -DskipTests
```

### Run Tests
```bash
cd appointement-service
mvn test
```

### View H2 Database Console
- URL: http://localhost:8087/h2-console
- Driver: `org.h2.Driver`
- JDBC URL: `jdbc:h2:mem:mykare`
- User: `sa`
- Password: (leave empty)

### View Kafka Topics
```bash
# If Kafka is running in Docker:
docker exec -it mykare-kafka-1 kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### Common Issues

**Issue**: Python worker can't connect to Kafka
```bash
# Solution: Make sure Kafka is running
# Check: KAFKA_BOOTSTRAP_SERVERS environment variable
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

**Issue**: "Unsupported connection setting MULTI_THREADED"
```bash
# Already fixed in application.yml - removed invalid parameter
```

**Issue**: "Timeout trying to lock table"
```bash
# Already fixed - increased LOCK_TIMEOUT=10000 in H2 URL
# Connection pooling configured with HikariCP
```

**Issue**: Frontend can't connect to backend
```bash
# Make sure backend is running on port 8087
# Check: http://localhost:8087/swagger-ui.html
```

---

## 📊 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        USER BROWSER                          │
│   Frontend UI (HTML/CSS/JS) - http://localhost:8087         │
│                                                               │
│  ┌──────────────┐  ┌─────────────┐  ┌────────────────────┐  │
│  │ Register/    │  │   Book      │  │  View              │  │
│  │ Login Form   │  │ Appointment │  │  Appointments      │  │
│  └──────────────┘  └─────────────┘  └────────────────────┘  │
└────────────┬────────────────────────────────────────────────┘
             │ JWT Token
             │ HTTP/REST
             ▼
┌─────────────────────────────────────────────────────────────┐
│     Spring Boot Backend (Java) - Port 8087                  │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Controllers (Auth, Appointment, Admin)                │ │
│  └─────────────┬──────────────────────┬──────────────────┘ │
│                │                      │                     │
│                ▼                      ▼                     │
│  ┌────────────────────────┐  ┌────────────────────────┐   │
│  │ Services               │  │ Kafka Producer         │   │
│  │ (Business Logic)       │  │ (Event Publisher)      │   │
│  └────────────┬───────────┘  └──────────┬─────────────┘   │
│               │                         │                   │
│               ▼                         ▼                   │
│  ┌────────────────────────────────────────────────────┐   │
│  │ H2 Database (In-Memory)                            │   │
│  │ - Users, Appointments, AppointmentLogs             │   │
│  └────────────────────────────────────────────────────┘   │
└─────────────────┬───────────────────────────────────────────┘
                  │
      ┌───────────┴────────────┐
      │                        │
      ▼                        ▼
┌──────────────────────┐  ┌─────────────────────────────────┐
│  KAFKA Topic:        │  │  Python Notification Worker    │
│  appointment-events  │  │  - Port 9092 (Kafka consumer)  │
│                      │  │  - Processes events            │
│                      │  │  - Logs notifications          │
│                      │  │  - Updates appointment status  │
└──────────────────────┘  └─────────────────────────────────┘
```

---

## 🔐 Security Features

✅ **JWT Authentication** - All endpoints require valid token  
✅ **Password Encryption** - BCrypt hashing  
✅ **Input Validation** - Prevents SQL injection & malicious input  
✅ **CORS Configured** - Same-origin requests only  
✅ **Security Headers** - Frame options, CSRF protection  
✅ **Error Handling** - No sensitive info leaked to clients  
✅ **Race Condition Prevention** - Pessimistic locking for slot booking  

---

## 📈 Performance Features

✅ **Connection Pooling** - HikariCP (10 max connections)  
✅ **Batch Processing** - Hibernate batching (size 20)  
✅ **Lock Timeout** - 10 seconds for concurrent operations  
✅ **Logging Optimization** - Disabled verbose SQL logging  
✅ **Request Validation** - Early rejection of invalid inputs  

---

## 📦 Deployment

### For Production:
1. Replace H2 with MySQL/PostgreSQL
2. Update `application.yml` with production database
3. Configure Kafka cluster
4. Set up Python worker on separate server
5. Use proper SSL/HTTPS certificates
6. Configure firewall rules
7. Set up logging and monitoring

### Environment Variables:
```bash
# Backend
DB_URL=jdbc:mysql://localhost:3306/mykare
DB_USERNAME=root
DB_PASSWORD=password
JWT_SECRET=your-secure-secret-key
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Python Worker
APPOINTMENT_SERVICE_URL=http://localhost:8087
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
LOG_LEVEL=INFO
```

---

## ✅ What's Ready

- ✅ Full authentication system with JWT
- ✅ Appointment CRUD operations
- ✅ Slot availability checking
- ✅ Event-driven architecture with Kafka
- ✅ Python notification worker
- ✅ Beautiful responsive UI
- ✅ Swagger API documentation
- ✅ Database console for debugging
- ✅ Security best practices implemented
- ✅ Race condition prevention
- ✅ Error handling and validation

---

## 🎯 Next Steps / Future Enhancements

1. **Notification Integration**
   - Email via SMTP
   - SMS via Twilio/AWS SNS
   - WhatsApp via Twilio
   - Push notifications

2. **Advanced Features**
   - Doctor profiles and availability
   - Appointment reschedule
   - Rating and reviews
   - Payment integration
   - Video consultation links

3. **Monitoring**
   - Application metrics (Micrometer)
   - Health checks
   - Distributed tracing
   - Log aggregation

4. **Scalability**
   - Kubernetes deployment
   - Load balancing
   - Caching (Redis)
   - Microservices separation

---

## 📞 Support

For issues or questions, check:
- Application logs in terminal
- H2 console for data inspection
- Swagger docs for API details
- Python worker logs for event processing

---

**Status**: ✅ **PRODUCTION READY FOR TESTING**  
**Last Updated**: 2026-07-10  
**Version**: 0.0.1-SNAPSHOT
