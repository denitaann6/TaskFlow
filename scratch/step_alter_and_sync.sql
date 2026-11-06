-- ====================================================================
-- TaskFlow: Database Table Structure Correction & Trigger Synchronization
-- ====================================================================

USE taskflow_db;

-- --------------------------------------------------------------------
-- STEP 1: Modify `user_tree_records` (Remove created_at)
-- --------------------------------------------------------------------
ALTER TABLE user_tree_records DROP COLUMN created_at;

-- --------------------------------------------------------------------
-- STEP 2: Modify `graph_adjacency_records` (Remove created_at)
-- --------------------------------------------------------------------
ALTER TABLE graph_adjacency_records DROP COLUMN created_at;

-- --------------------------------------------------------------------
-- STEP 3: Remove stale test dependency records (101->102 and 101->103)
-- --------------------------------------------------------------------
DELETE FROM task_dependencies WHERE prerequisite_id = 101 AND dependent_id IN (102, 103);
DELETE FROM graph_adjacency_records WHERE prerequisite_task_id = 101 AND dependent_task_id IN (102, 103);

-- --------------------------------------------------------------------
-- STEP 4: Create Synchronization Triggers on `task_dependencies`
-- --------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_task_dependencies_after_insert;
DROP TRIGGER IF EXISTS trg_task_dependencies_after_delete;
DROP TRIGGER IF EXISTS trg_task_dependencies_after_update;

DELIMITER $$

CREATE TRIGGER trg_task_dependencies_after_insert
AFTER INSERT ON task_dependencies
FOR EACH ROW
BEGIN
    INSERT IGNORE INTO graph_adjacency_records (prerequisite_task_id, dependent_task_id)
    VALUES (NEW.prerequisite_id, NEW.dependent_id);
END$$

CREATE TRIGGER trg_task_dependencies_after_delete
AFTER DELETE ON task_dependencies
FOR EACH ROW
BEGIN
    DELETE FROM graph_adjacency_records
    WHERE prerequisite_task_id = OLD.prerequisite_id
      AND dependent_task_id = OLD.dependent_id;
END$$

CREATE TRIGGER trg_task_dependencies_after_update
AFTER UPDATE ON task_dependencies
FOR EACH ROW
BEGIN
    DELETE FROM graph_adjacency_records
    WHERE prerequisite_task_id = OLD.prerequisite_id
      AND dependent_task_id = OLD.dependent_id;

    INSERT IGNORE INTO graph_adjacency_records (prerequisite_task_id, dependent_task_id)
    VALUES (NEW.prerequisite_id, NEW.dependent_id);
END$$

DELIMITER ;

