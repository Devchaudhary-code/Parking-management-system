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
