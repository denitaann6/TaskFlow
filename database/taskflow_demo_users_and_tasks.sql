-- ============================================================================
-- TaskFlow: Clean Test Tasks and Add Demo Working User Accounts & Tasks
-- Target Database: taskflow_db (MySQL 8.0)
-- Location: database/taskflow_demo_users_and_tasks.sql
-- ============================================================================

USE taskflow_db;

-- ----------------------------------------------------------------------------
-- STEP 1: PREVIEW TARGET TEST TASKS TO BE REMOVED
-- Confirm that records match IDs and names before executing destructive changes.
-- ----------------------------------------------------------------------------
SELECT id, name, member, priority, status
FROM tasks
WHERE id IN (111, 112, 114, 115);

-- ----------------------------------------------------------------------------
-- STEP 2: TRANSACTION TO SAFELY REMOVE TEST TASKS & INSERT DEMO ACCOUNTS/TASKS
-- ----------------------------------------------------------------------------
START TRANSACTION;

-- A. Safe deletion of ONLY the four confirmed test task records
-- Ensures both ID and Name match precisely, preventing unintended data deletion.
DELETE FROM tasks
WHERE (id = 111 AND name = 'Test A')
   OR (id = 112 AND name = 'Test B')
   OR (id = 114 AND name = 'a')
   OR (id = 115 AND name = 'b');

-- B. Insert 4 Realistic Demo MEMBER Accounts
-- Password: Member@123
-- Stored in BCrypt format ($2a$10$...) as required by Spring Security BCryptPasswordEncoder.
-- Reuses existing application authentication and authorization rules (Role: MEMBER).
INSERT INTO users (username, password_hash, display_name, role, active, assigned_member_name)
VALUES
  ('arjun', '$2a$10$29V/TfRJhfsLNLWMf7VG/uW6kwROihSOlGsklbMyi/lechXuoZ0um', 'Arjun', 'MEMBER', 1, 'Arjun'),
  ('priya', '$2a$10$29V/TfRJhfsLNLWMf7VG/uW6kwROihSOlGsklbMyi/lechXuoZ0um', 'Priya', 'MEMBER', 1, 'Priya'),
  ('rahul', '$2a$10$29V/TfRJhfsLNLWMf7VG/uW6kwROihSOlGsklbMyi/lechXuoZ0um', 'Rahul', 'MEMBER', 1, 'Rahul'),
  ('sneha', '$2a$10$29V/TfRJhfsLNLWMf7VG/uW6kwROihSOlGsklbMyi/lechXuoZ0um', 'Sneha', 'MEMBER', 1, 'Sneha');

-- C. Insert 4 Realistic Demo Tasks with Status 'Pending'
-- Assigned to the newly created member accounts with valid priorities and required schema columns.
INSERT INTO tasks (id, name, member, priority, status)
VALUES
  (118, 'Prepare Database Documentation(overall)', 'Arjun', 'High', 'Pending'),
  (119, 'Design User Interface', 'Priya', 'High', 'Pending'),
  (120, 'Test Authentication Module', 'Rahul', 'Medium', 'Pending'),
  (121, 'Prepare Project Report', 'Sneha', 'Medium', 'Pending');

COMMIT;

-- ----------------------------------------------------------------------------
-- STEP 3: VERIFICATION QUERIES
-- ----------------------------------------------------------------------------

-- Verify that the test tasks (111, 112, 114, 115) are gone
SELECT id, name, member, priority, status
FROM tasks
WHERE id IN (111, 112, 114, 115);

-- Verify all existing and new user accounts
SELECT id, username, display_name, role, active, assigned_member_name
FROM users
ORDER BY id;

-- Verify all current tasks in MySQL
SELECT id, name, member, priority, status
FROM tasks
ORDER BY id;

-- Verify Pending Queue tasks (all tasks awaiting execution)
SELECT id, name, member, priority, status
FROM tasks
WHERE status = 'Pending'
ORDER BY id;
