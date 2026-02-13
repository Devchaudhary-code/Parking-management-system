# Parking Management System — Desktop Client + REST Backend

A production-style **Parking Management System** built with a clean separation between a **Java desktop client (Swing)** and a **Spring Boot REST backend** backed by **MySQL**.  
The backend centralizes validation, pricing rules, and persistence; the desktop client focuses purely on UI and user workflow.

---

## Overview

### Key capabilities
- Ticket creation on **entry** (license plate + vehicle type)
- Live view of **active (open)** tickets and **closed** ticket history
- Ticket closing on **exit**
- Automatic **fee calculation** based on **vehicle type + parking duration**
- Persistent storage of ticket details (entry/exit timestamps, total duration, computed price, status)

### System workflow
**Desktop Client (Swing) → HTTP (REST) → Spring Boot API → JPA/Hibernate → MySQL**

- The desktop client **does not** connect to MySQL directly.
- The backend acts as the single source of truth for business rules and data access.

---

## Tech Stack
- **Java (Swing)** — desktop UI client
- **Spring Boot** — REST API layer
- **Spring Data JPA / Hibernate** — persistence + ORM
- **MySQL** — relational database
- **Maven** — build & dependency management
- **Git/GitHub** — version control

---
