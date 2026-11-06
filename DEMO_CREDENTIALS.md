# TaskFlow — Demo Account Credentials

This document provides login credentials for academic demonstration and viva evaluation.

> **Security Note:** In MySQL database, all passwords are encrypted using BCrypt one-way hashing with salt. No plaintext passwords exist in the database or client code.

---

## 1. System Administrator Account
The administrator has full authority over all tasks, team members, scheduling, and system-wide reports.

| Username | Password | Role | Display Name | Permissions |
| :--- | :--- | :--- | :--- | :--- |
| `admin` | `Admin@123` | **ADMIN** | System Administrator | Full CRUD on tasks, task assignment, team overview, all reports, all DS lab features |

---

## 2. Predefined Team Member Accounts
Each member account maps directly to existing task assignments in `taskflow_db`. Members can view their assigned tasks, update task status (Pending, In Progress, Completed), and view personal progress.

| Username | Password | Role | Display Name | Mapped Tasks in DB |
| :--- | :--- | :--- | :--- | :--- |
| `john` | `Member@123` | **MEMBER** | John | Task 101 ("Database Design") |
| `subu` | `Member@123` | **MEMBER** | Subu | Task 102 ("trial"), Task 116 ("Prepare Report (Phase 1)") |
| `sheena` | `Member@123` | **MEMBER** | Sheena | Task 103 ("Bug Checking") |
| `sebastian` | `Member@123` | **MEMBER** | Sebastian | Task 104 ("backend") |
| `mary` | `Member@123` | **MEMBER** | Mary | Task 105 ("Report Writing") |
| `mathew` | `Member@123` | **MEMBER** | Mathew | Task 106 ("Documentation"), Task 117 ("Prepare Report (Phase 2)") |
| `amrith` | `Member@123` | **MEMBER** | Amrith | Task 110 ("backend testing") |
| `testuser` | `Member@123` | **MEMBER** | Test User | Task 108 ("Testing Task") |
| `arjun` | `Member@123` | **MEMBER** | Arjun | Task 118 ("Prepare Database Documentation(overall)") |
| `priya` | `Member@123` | **MEMBER** | Priya | Task 119 ("Design User Interface") |
| `rahul` | `Member@123` | **MEMBER** | Rahul | Task 120 ("Test Authentication Module") |
| `sneha` | `Member@123` | **MEMBER** | Sneha | Task 121 ("Prepare Project Report") |

---

## 3. Role-Based Permission Matrix

| Action | Administrator (`ADMIN`) | Member (`MEMBER`) |
| :--- | :---: | :---: |
| **View All Tasks** | ✅ Allowed | ❌ Restricted (Own tasks only) |
| **Create New Tasks** | ✅ Allowed | ❌ Forbidden (HTTP 403) |
| **Edit Task Details** | ✅ Allowed | ❌ Forbidden (HTTP 403) |
| **Delete Tasks** | ✅ Allowed | ❌ Forbidden (HTTP 403) |
| **Update Own Task Status** | ✅ Allowed | ✅ Allowed |
| **Update Other Member's Status (IDOR)** | ✅ Allowed | ❌ Forbidden (HTTP 403) |
| **Complete Own Task** | ✅ Allowed | ✅ Allowed |
| **Complete Other Member's Task (IDOR)** | ✅ Allowed | ❌ Forbidden (HTTP 403) |
| **View Organization Workload Report** | ✅ Allowed | ❌ Restricted (Own stats only) |
| **Pending Queue (FIFO) API & Page** | ✅ Allowed | ❌ Forbidden (HTTP 403 & Pre-render Guard) |
| **Completed Tasks Stack (LIFO) API & Page** | ✅ Allowed | ❌ Forbidden (HTTP 403 & Pre-render Guard) |
| **Popped Tasks History Audit Log** | ✅ Allowed | ❌ Forbidden (HTTP 403) |
| **Process Next / Dequeue Tasks** | ✅ Allowed | ❌ Forbidden (HTTP 403) |
| **Pop Completed Tasks** | ✅ Allowed | ❌ Forbidden (HTTP 403) |
| **Data Structures Lab Algorithms** | ✅ Allowed | ✅ Allowed (Scoped to authorized tasks) |
