# MyKare Appointment Service

Spring Boot backend for appointment booking with JWT authentication, role-based access, MySQL/H2 persistence, Swagger documentation, and Kafka appointment notifications.

## Tech Stack

- Java 21
- Spring Boot 4.1
- Spring Web MVC
- Spring Security with JWT
- Spring Data JPA
- MySQL for local infrastructure
- H2 for quick local runs and tests
- Apache Kafka
- Python notification worker
- Swagger/OpenAPI

## Features

- Register and login users.
- Roles: `PATIENT`, `DOCTOR`, `ADMIN`.
- Create, view, update, cancel/complete, and delete appointments.
- Fetch available doctor slots.
- View appointment logs/history.
- Prevent double booking for the same doctor at the same appointment time.
- Publish appointment events to Kafka.
- Consume appointment events in the Python notification worker and update notification processing status.
- Consistent validation and error responses.
- Simple browser UI to demonstrate the workflow.

## Project Structure

```text
mykare/
  appointement-service/
    src/main/java/com/appointementservice/
      config/
      controller/
      dto/
      entity/
      exception/
      repository/
      security/
      service/
    src/main/resources/application.yml
    pom.xml
  notification-worker/
    docker-compose.yml
    main.py
    requirement.txt
```

## Quick Run With H2

Use this when you want to run the backend without Docker or MySQL.

```bash
cd appointement-service
mvn spring-boot:run
```

Application:

```text
http://localhost:8087
```

Swagger UI:

```text
http://localhost:8087/swagger-ui/index.html
```

H2 Console:

```text
http://localhost:8087/h2-console
JDBC URL: jdbc:h2:mem:mykare
User: sa
Password:
```

## Run With MySQL And Kafka

Start MySQL and Kafka:

```bash
cd notification-worker
docker compose up -d
```

Run the Spring Boot service with MySQL:

```powershell
cd ..\appointement-service
$env:DB_URL="jdbc:mysql://localhost:3306/healthcare_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="mykare"
$env:DB_PASSWORD="mykare"
$env:DB_DRIVER="com.mysql.cj.jdbc.Driver"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
mvn spring-boot:run
```

Default Docker database values:

```text
Database: healthcare_db
Username: mykare
Password: mykare
Root password: root
Port: 3306
```

## Authentication Flow

Register patient:

```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "Sanjeev",
  "email": "sanjeev@example.com",
  "password": "password123",
  "role": "PATIENT"
}
```

Login:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "sanjeev@example.com",
  "password": "password123"
}
```

Use the returned token:

```text
Authorization: Bearer <token>
```

## Appointment APIs

Create appointment. Allowed roles: `PATIENT`, `ADMIN`.

```http
POST /api/appointments
Authorization: Bearer <token>
Content-Type: application/json

{
  "patientName": "Sanjeev",
  "patientEmail": "sanjeev@example.com",
  "doctorName": "Dr Rao",
  "appointmentTime": "2026-07-12T10:30:00",
  "reason": "Fever",
  "notes": "First visit"
}
```

List and filter:

```text
GET /api/appointments
GET /api/appointments/{id}
GET /api/appointments?patientEmail=sanjeev@example.com
GET /api/appointments?doctorName=Dr Rao
GET /api/appointments/available-slots?doctorName=Dr Rao&date=2026-07-12
GET /api/appointments/{id}/logs
```

Update appointment. Allowed roles: `PATIENT`, `ADMIN`.

```text
PUT /api/appointments/{id}
```

Update status. Allowed roles: `DOCTOR`, `ADMIN`.

```http
PATCH /api/appointments/{id}/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "COMPLETED"
}
```

Delete appointment. Allowed role: `ADMIN`.

```text
DELETE /api/appointments/{id}
```

Cancel appointment. Allowed roles: `PATIENT`, `ADMIN`.

```text
PATCH /api/appointments/{id}/cancel
```

## Notification Worker

Install Python dependencies:

```bash
cd notification-worker
pip install -r requirement.txt
```

Run worker:

```bash
python main.py
```

The worker consumes the Kafka topic `appointment-events`, logs the notification message, and calls the Spring Boot service to update notification status from `PROCESSING` to `SENT`.

Optional worker environment variable:

```text
APPOINTMENT_SERVICE_URL=http://localhost:8087
```

In a real production system, the `send_notification` function can be replaced with email, SMS, or WhatsApp integration.

## Database Schema

Main tables:

```text
app_users
- id
- name
- email
- password
- role
- created_at

appointments
- id
- patient_name
- patient_email
- doctor_name
- appointment_time
- reason
- notes
- status
- notification_status
- created_at
- updated_at

appointment_logs
- id
- appointment_id
- action
- message
- created_at
```

The `appointments` table has a uniqueness rule for doctor, appointment time, and status to help prevent duplicate active bookings.

## Testing

Run tests:

```bash
cd appointement-service
mvn test
```

Current status:

```text
Tests run: 1
Failures: 0
Errors: 0
Build: SUCCESS
```

## Important Configuration

Main config file:

```text
appointement-service/src/main/resources/application.yml
```

Useful environment variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
DB_DRIVER
KAFKA_BOOTSTRAP_SERVERS
APPOINTMENT_TOPIC
JWT_SECRET
JWT_EXPIRATION_MINUTES
```
