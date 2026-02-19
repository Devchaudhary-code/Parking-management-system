# Parking Management System (Spring Boot + Java Swing)

A Parking Management System built with a **Spring Boot REST backend** and a **Java Swing desktop client**. The current client focuses on the **History/Dashboard** module with **plate search, filtering, and sorting**, and includes a **login screen** before accessing the dashboard.

---

## Features

### Swing Client
- Login screen (Admin access)
- History table view (ID, Plate, Type, Status, Entry Time, Exit Time, Amount)
- Plate search (contains)
- Filter by Status (OPEN/CLOSED) and Vehicle Type
- Sort by Exit Time / Entry Time / Plate (A–Z, Z–A)
- Non-blocking UI using background requests (SwingWorker)

### Backend (Spring Boot)
- REST API for ticket history retrieval with query parameters:
  - `plate` (contains)
  - `status` (OPEN/CLOSED)
  - `vehicleType` (CAR/BIKE/TRUCK/OTHER)
  - `sort` (e.g., `exitTime,desc`)

---

## Tech Stack
- Java (Swing)
- Spring Boot
- Spring Data JPA
- MySQL
- Git/GitHub

---

## Project Structure
Parking_final/
pom.xml
parking-backend/
parking-swing-client/
historic.sql


---

## Prerequisites
- Java JDK (project currently uses **JDK 25**)
- MySQL Server
- (Optional) IntelliJ IDEA

> Maven is not required if you use the Maven Wrapper (`mvnw` / `mvnw.cmd`).

---

## Database Setup (MySQL)

1) Create a database:
```sql
CREATE DATABASE parking_db;
(Optional) Import sample data:

Open MySQL Workbench

Run historic.sql (if included)

Configure backend DB connection:
Open:
parking-backend/src/main/resources/application.properties

Example:

spring.datasource.url=jdbc:mysql://localhost:3306/parking_db
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
Run the Backend (Spring Boot)
Using Maven Wrapper (recommended)
From repo root:

.\mvnw -pl parking-backend spring-boot:run
Backend runs on:

http://localhost:8080

Run the Swing Client
Recommended: run from IntelliJ

Open the project (root pom.xml)

Run ParkingHistoryUI.main() (it launches LoginUI first)

Login Credentials
Current login is hardcoded for demo purposes:

Username: admin

Password: admin123

(You can change this in LoginUI.java.)

API Reference
Base URL:
http://localhost:8080/api/tickets

Get ticket history
GET /api/tickets

Optional query params:

plate=MH12

status=OPEN or CLOSED

vehicleType=CAR (BIKE/TRUCK/OTHER)

sort=exitTime,desc (or entryTime,asc, plate,desc, etc.)

Example:

curl "http://localhost:8080/api/tickets?status=CLOSED&sort=exitTime,desc"
GitHub Notes (Important)
Do NOT commit build/IDE files:

target/

.idea/

*.iml

*.class

Recommended .gitignore:

target/
*.class
.idea/
*.iml
out/
Authors
Nur Aleesa Rizal

Charvi Sharma

Dev Chaudhary

Palaksha Bora

Daksh Patel
