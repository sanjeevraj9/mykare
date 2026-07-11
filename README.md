# MyKare Backend Assignment

This repository contains two parts:

- `appointement-service`: Spring Boot REST API for appointment booking.
- `notification-worker`: Python Kafka consumer for appointment notifications plus Docker Compose for MySQL and Kafka.

Start with the detailed backend README:

```text
appointement-service/README.md
```

Quick commands:

```bash
cd notification-worker
docker compose up -d
```

```powershell
cd ..\appointement-service
$env:DB_URL="jdbc:mysql://localhost:3306/healthcare_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="mykare"
$env:DB_PASSWORD="mykare"
$env:DB_DRIVER="com.mysql.cj.jdbc.Driver"
mvn spring-boot:run
```

Swagger:

```text
http://localhost:8087/swagger-ui/index.html
```

Simple workflow UI:

```text
http://localhost:8087
```
