# 🧪 MyKare System - Complete Testing Guide

**Status**: ✅ READY FOR TESTING  
**Last Updated**: 2026-07-10  
**Test Duration**: ~15 minutes

---

## 📋 Pre-Test Checklist

Before starting, verify:
- [ ] Java is installed (OpenJDK 21+)
- [ ] Python 3.8+ is installed
- [ ] Maven build completed successfully
- [ ] JAR file exists: `appointement-service/target/appointement-service-0.0.1-SNAPSHOT.jar`
- [ ] Python requirements installed

---

## 🚀 Starting the System

### Method 1: Automated (Recommended for Windows)
```
Double-click: start-all.bat
```

### Method 2: Manual Startup (Complete Control)

**Terminal 1 - Backend**:
```bash
cd appointement-service
java -jar target/appointement-service-0.0.1-SNAPSHOT.jar
```

Wait for: `Tomcat started on port(s): 8087`

**Terminal 2 - Python Worker**:
```bash
cd notification-worker
pip install -r requirement.txt
python main.py
```

Look for: `Subscribed to topic: appointment-events`

---

## ✅ Test Scenario 1: User Registration

### Steps
1. Open browser: http://localhost:8087
2. Fill Registration Form:
   - **Name**: Sanjeev Kumar
   - **Email**: sanjeev@example.com
   - **Password**: Test@123
   - **Role**: PATIENT
3. Click **REGISTER** button

### Expected Result
```
✅ Success! User registered successfully. Please login.
```

### What's Happening Behind the Scenes
- Backend validates input (non-null, email format)
- Password is hashed using BCrypt
- User record created in H2 database
- No errors in backend console

---

## ✅ Test Scenario 2: User Login

### Steps
1. On same page, scroll to Login section
2. Fill Login Form:
   - **Email**: sanjeev@example.com
   - **Password**: Test@123
3. Click **LOGIN** button

### Expected Result
```
✅ Login successful! Welcome, Sanjeev Kumar
```
*Note: Page will show authenticated sections*

### What's Happening
- Backend validates credentials
- JWT token generated (120 min expiration)
- Token stored in browser localStorage
- All API calls will include this token

### Verify Token in Browser
Open browser DevTools (F12) → Application → Local Storage:
```
Key: jwt_token
Value: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## ✅ Test Scenario 3: Book Appointment

### Steps
1. Scroll to "Book Appointment" form
2. Fill in details:
   - **Patient Name**: Sanjeev Kumar
   - **Patient Email**: sanjeev@example.com
   - **Doctor Name**: Dr Rao
   - **Date**: Tomorrow (tomorrow's date)
   - **Time**: 10:00 AM
   - **Reason for Visit**: Fever with cough
   - **Additional Notes**: First time visit, 1 year fever
3. Click **BOOK APPOINTMENT** button

### Expected Result
```
✅ Appointment booked successfully!
Appointment ID: 1
Status: BOOKED
Doctor: Dr Rao
Date & Time: 2026-07-11 10:00:00
```

### What's Happening Backend
1. **API Call**: POST /api/appointments (with JWT token)
2. **Validation**: Input sanitization and business logic checks
3. **Database**: Appointment record created in `appointments` table
4. **Event Publishing**: Kafka event sent to `appointment-events` topic
5. **Response**: Appointment ID returned to frontend
6. **Database Log**: Entry created in `appointment_logs` table

### Backend Console Should Show
```
[INFO] Appointment created: AppointmentEvent(...)
[INFO] Publishing event to Kafka topic: appointment-events
```

---

## ✅ Test Scenario 4: Python Worker Processing

### Steps
1. Look at Python Worker terminal (should still be running)
2. After booking appointment, you should see:

### Expected Output in Python Terminal
```
Processing appointment event from Kafka...
Appointment ID: 1
Patient: Sanjeev Kumar (sanjeev@example.com)
Doctor: Dr Rao
Time: 2026-07-11T10:00:00
Reason: Fever with cough

[NOTIFICATION] Sending notification for appointment 1
Status: PROCESSING → Updating backend...
Status: PROCESSING → Update successful
Status: SENDING_NOTIFICATION
[NOTIFICATION] Appointment notification sent successfully!
Status: SENT → Final update...
Status: SENT → Update successful
```

### What's Happening
1. **Kafka Consumer**: Python worker listening to `appointment-events` topic
2. **Event Reception**: Appointment event received from Kafka
3. **Status Update 1**: Updates appointment to `PROCESSING` via REST API call
4. **Notification Simulation**: Logs that notification would be sent (SMS/Email/WhatsApp)
5. **Status Update 2**: Updates appointment to `SENT` via REST API call
6. **Database Update**: `notification_status` field updated in database

---

## ✅ Test Scenario 5: View Appointment History

### Steps
1. On webpage, find "Appointments & History" section
2. In the search box, enter Appointment ID: **1**
3. Click **VIEW HISTORY** button

### Expected Result
```
Appointment Details:
ID: 1
Patient: Sanjeev Kumar (sanjeev@example.com)
Doctor: Dr Rao
Date & Time: 2026-07-11 10:00:00
Reason: Fever with cough
Notes: First time visit, 1 year fever
Status: BOOKED
Notification Status: SENT

Appointment Log:
├─ [10:15:23] APPOINTMENT_CREATED - Appointment booked
├─ [10:15:24] NOTIFICATION_PROCESSING - Notification processing started
├─ [10:15:25] NOTIFICATION_SENT - Notification sent to patient
└─ [10:15:26] STATUS_UPDATED - Appointment status updated
```

### What's Happening
- GET /api/appointments/1/logs API called
- Appointment logs retrieved from database
- All status transitions displayed in chronological order

---

## ✅ Test Scenario 6: Cancel Appointment

### Steps
1. In the history section, find the Cancel button
2. Click **CANCEL APPOINTMENT**
3. Confirm in the popup dialog

### Expected Result
```
✅ Appointment cancelled successfully!
Status updated to: CANCELLED
```

### Backend Update
- Appointment status changed from `BOOKED` to `CANCELLED`
- New log entry created: `APPOINTMENT_CANCELLED`
- Notification worker may be triggered for cancellation notification

---

## ✅ Test Scenario 7: Check Available Slots

### Steps
1. Scroll to "Available Slots" section
2. Enter:
   - **Doctor Name**: Dr Rao
   - **Date**: 2026-07-12 (some future date)
3. Click **GET SLOTS** button

### Expected Result
```
Available Slots for Dr Rao on 2026-07-12:
├─ 09:00 AM
├─ 09:30 AM
├─ 10:00 AM
├─ 10:30 AM
├─ 11:00 AM
... (continues until 5 PM, 30-min intervals)
```

### What's Happening
- API Call: GET /api/appointments/available-slots
- Query: Appointments not booked for Dr Rao on that date
- Logic: Generates 30-minute slots from 9 AM to 5 PM
- Returns: All free slots

---

## ✅ Test Scenario 8: Multiple Concurrent Bookings (Race Condition Test)

### Purpose
Verify system handles multiple simultaneous bookings correctly

### Steps
1. Open 2 browser windows on http://localhost:8087
2. Login to both with same user (or different users)
3. **Window 1**: Try to book slot "10:00 AM" tomorrow
4. **Window 2**: Try to book same slot "10:00 AM" tomorrow
5. Click **BOOK APPOINTMENT** on both at roughly the same time

### Expected Result
- ✅ One booking succeeds
- ✅ Other booking fails with: "Slot not available - already booked"

### Why This Matters
- Prevents double-booking same appointment slot
- Uses database-level locking
- Important for healthcare system reliability

---

## ✅ Test Scenario 9: API Documentation

### Steps
1. Open: http://localhost:8087/swagger-ui.html
2. Explore available endpoints
3. Try "Try it Out" on a few endpoints

### What You'll See
- All REST API endpoints listed
- Request/Response schemas
- Authentication requirements
- Try-it-out capability with actual backend

---

## ✅ Test Scenario 10: Database Console Access

### Steps
1. Open: http://localhost:8087/h2-console
2. Connection details:
   - **JDBC URL**: `jdbc:h2:mem:mykare`
   - **User Name**: `sa`
   - **Password**: (leave empty)
3. Click **CONNECT**

### What You Can Do
- View actual database tables
- Check data in `app_user`, `appointments`, `appointment_logs`
- Run SQL queries
- Verify data integrity

### Sample Queries
```sql
-- View all appointments
SELECT * FROM appointments;

-- View appointment logs
SELECT * FROM appointment_logs WHERE appointment_id = 1;

-- View users
SELECT id, name, email, role FROM app_user;

-- Check appointment status changes
SELECT appointment_id, action, message, created_at 
FROM appointment_logs 
ORDER BY created_at DESC;
```

---

## 📊 Testing Checklist

Use this checklist to track your testing progress:

### Frontend Tests
- [ ] Register user with valid data
- [ ] Register fails with invalid email
- [ ] Register fails with existing email
- [ ] Login succeeds with correct credentials
- [ ] Login fails with wrong password
- [ ] Logout clears token
- [ ] Forms show validation errors

### Appointment Tests
- [ ] Book appointment successfully
- [ ] Appointment ID displayed
- [ ] View appointment history
- [ ] Cancel appointment works
- [ ] Cancelled status reflected in UI
- [ ] Available slots show correctly
- [ ] Cannot book same slot twice

### Backend Tests
- [ ] GET /api/auth/register → 201 Created
- [ ] POST /api/auth/login → 200 OK with token
- [ ] POST /api/appointments → 201 Created (with JWT)
- [ ] GET /api/appointments → 200 OK
- [ ] GET /api/appointments/available-slots → 200 OK
- [ ] GET /api/appointments/{id}/logs → 200 OK
- [ ] PATCH /api/appointments/{id}/cancel → 200 OK
- [ ] POST without JWT token → 401 Unauthorized

### Database Tests
- [ ] User records created correctly
- [ ] Appointment records saved
- [ ] Appointment logs added
- [ ] Status updates reflected
- [ ] No duplicate appointments

### Python Worker Tests
- [ ] Worker starts without errors
- [ ] Worker connects to Kafka
- [ ] Worker receives appointment events
- [ ] Worker updates appointment status
- [ ] Worker processes events end-to-end

### Security Tests
- [ ] JWT token valid for 120 minutes
- [ ] Expired token rejected
- [ ] Invalid token rejected
- [ ] No SQL injection possible
- [ ] No cross-site scripting possible
- [ ] API requires authentication
- [ ] Password properly hashed in database

---

## 🐛 Troubleshooting During Tests

### Problem: "Connection refused" on http://localhost:8087
```
Solution: Backend not started
Action: Check if Java process is running
Command: java -jar target/appointement-service-0.0.1-SNAPSHOT.jar
```

### Problem: "Unauthorized" after login
```
Solution: JWT token not sent or expired
Action: Check browser Local Storage for jwt_token
Debug: Clear localStorage and login again
```

### Problem: "Appointment already booked"
```
This is CORRECT behavior!
Means: Race condition prevention is working
You cannot book same slot twice
```

### Problem: Python worker doesn't show notification logs
```
Solution: Kafka not running or worker crashed
Check: Python worker terminal for error messages
Debug: Verify Kafka connection settings
```

### Problem: Slot already booked for tomorrow
```
This is NORMAL - system may have demo appointments
Solution: Pick different doctor or different date
Or: Check H2 console and delete test appointments
```

---

## 📈 Performance Metrics to Monitor

During testing, check:

### Response Times
- **Login**: Should be < 500ms
- **Book Appointment**: Should be < 1s
- **View History**: Should be < 500ms
- **Get Slots**: Should be < 500ms

### Database
- **Connection Pool**: Max 10 connections
- **Batch Size**: 20 inserts/updates
- **Lock Timeout**: 10 seconds (very high for safety)

### Kafka
- **Message Delivery**: Within 100ms (network dependent)
- **Worker Processing**: < 2 seconds end-to-end

---

## ✨ Success Criteria

Your system is working perfectly when:

✅ User can register  
✅ User can login and get JWT token  
✅ User can book appointment  
✅ Python worker receives Kafka event  
✅ Python worker updates appointment status  
✅ Status changes visible in appointment history  
✅ Cannot book duplicate appointments  
✅ All APIs require authentication  
✅ Database reflects all changes  
✅ No error messages in console logs  

---

## 📞 Getting Help

If something doesn't work:

1. **Check Backend Console**: Look for error messages
2. **Check Python Console**: Look for Kafka or HTTP errors
3. **Check Browser Console**: F12 → Console tab
4. **Check Database**: http://localhost:8087/h2-console
5. **Check Logs**: Look at terminal output
6. **Read**: PROJECT_COMPLETE.md and README_SETUP.md

---

## 🎉 What's Next?

After successful testing:

1. **Deploy**: Use Docker or production environment
2. **Enhance**: Add SMS/Email notifications
3. **Scale**: Set up MySQL instead of H2
4. **Integrate**: Add external services (payment, analytics)
5. **Monitor**: Set up logging and alerting

---

**Estimated Test Time**: 15-20 minutes  
**Difficulty Level**: Easy  
**Success Rate**: >95% if system set up correctly  

**Happy Testing! 🚀**
