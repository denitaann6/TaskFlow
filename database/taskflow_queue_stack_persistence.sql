-- ============================================================================
-- TaskFlow: Persistence Migration for Queue, Stack & Popped History
-- Target Database: taskflow_db (MySQL 8.0)
-- Location: database/taskflow_queue_stack_persistence.sql
-- ============================================================================

USE taskflow_db;

-- ----------------------------------------------------------------------------
-- 1. CREATE task_queue TABLE (FIFO Queue Persistence)
-- Holds tasks with status 'Pending' awaiting execution.
-- Lowest sequence_order = FRONT (earliest arrival, next to process).
-- Highest sequence_order = REAR (latest arrival).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS task_queue (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    sequence_order BIGINT NOT NULL,
    enqueued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_task_queue_task_id UNIQUE (task_id),
    CONSTRAINT fk_task_queue_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    INDEX idx_queue_sequence (sequence_order ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 2. CREATE completed_task_stack TABLE (LIFO Stack Persistence)
-- Holds active audit entries for tasks with status 'Completed'.
-- Highest stack_order = TOP (most recently completed task).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS completed_task_stack (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    stack_order BIGINT NOT NULL,
    pushed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_completed_stack_task_id UNIQUE (task_id),
    CONSTRAINT fk_completed_stack_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    INDEX idx_stack_order (stack_order DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 3. CREATE popped_tasks_history TABLE (Durable Popped Tasks Audit Log)
-- Records every successful POP event separately from the active stack.
-- Survives page refresh, restart, and synchronization.
-- Does not alter the original task record or its 'Completed' status.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS popped_tasks_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    task_name VARCHAR(150) NOT NULL,
    member VARCHAR(100) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Completed',
    popped_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    popped_by VARCHAR(100) DEFAULT 'Administrator',
    original_stack_order BIGINT,
    CONSTRAINT fk_popped_history_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    INDEX idx_popped_at (popped_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 4. INITIAL SEEDING (Idempotent: Only seeds if tables are empty)
-- Populates existing 'Pending' tasks into task_queue in ascending order of ID.
-- Populates existing 'Completed' tasks into completed_task_stack in order of ID.
-- ----------------------------------------------------------------------------
START TRANSACTION;

-- Seed task_queue with current Pending tasks
INSERT INTO task_queue (task_id, sequence_order, enqueued_at)
SELECT t.id, ROW_NUMBER() OVER (ORDER BY t.id ASC) AS seq, NOW()
FROM tasks t
WHERE t.status = 'Pending'
  AND NOT EXISTS (SELECT 1 FROM task_queue tq WHERE tq.task_id = t.id);

-- Seed completed_task_stack with current Completed tasks
INSERT INTO completed_task_stack (task_id, stack_order, pushed_at)
SELECT t.id, ROW_NUMBER() OVER (ORDER BY t.id ASC) AS s_ord, NOW()
FROM tasks t
WHERE t.status = 'Completed'
  AND NOT EXISTS (SELECT 1 FROM completed_task_stack cts WHERE cts.task_id = t.id);

COMMIT;

-- ----------------------------------------------------------------------------
-- 5. VERIFICATION QUERIES
-- ----------------------------------------------------------------------------
-- Verify table schemas
DESCRIBE task_queue;
DESCRIBE completed_task_stack;
DESCRIBE popped_tasks_history;

-- Verify initial queue contents (FIFO order: lowest sequence_order first)
SELECT tq.id AS queue_entry_id, tq.task_id, t.name, t.member, t.priority, t.status, tq.sequence_order, tq.enqueued_at
FROM task_queue tq
JOIN tasks t ON tq.task_id = t.id
ORDER BY tq.sequence_order ASC;

-- Verify initial stack contents (LIFO order: highest stack_order on top)
SELECT cts.id AS stack_entry_id, cts.task_id, t.name, t.member, t.priority, t.status, cts.stack_order, cts.pushed_at
FROM completed_task_stack cts
JOIN tasks t ON cts.task_id = t.id
ORDER BY cts.stack_order DESC;

-- Verify popped history (empty initially until pop operations are performed)
SELECT * FROM popped_tasks_history ORDER BY popped_at DESC;
