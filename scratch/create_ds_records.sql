-- ====================================================================
-- TaskFlow Phase 2: MySQL Data Structure Records Schema & Population
-- Tables: user_tree_records, graph_adjacency_records
-- ====================================================================

USE taskflow_db;

-- --------------------------------------------------------------------
-- STEP 1: Table `user_tree_records`
-- Purpose: Store the actual Binary Search Tree structure generated
-- from existing team members using the median-balanced BST algorithm.
-- --------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS user_tree_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    parent_user_id INT NULL,
    left_child_id INT NULL,
    right_child_id INT NULL,
    tree_version VARCHAR(50) NOT NULL DEFAULT 'v1.0',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_tree_node UNIQUE (tree_version, user_id),
    CONSTRAINT fk_user_tree_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_tree_parent FOREIGN KEY (parent_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_user_tree_left FOREIGN KEY (left_child_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_user_tree_right FOREIGN KEY (right_child_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Populate `user_tree_records` with the 17 users from the median-balanced BST
INSERT INTO user_tree_records (user_id, parent_user_id, left_child_id, right_child_id, tree_version, created_at) VALUES
(1, 2, NULL, NULL, 'v1.0', NOW()),
(2, 4, 1, 3, 'v1.0', NOW()),
(3, 2, NULL, NULL, 'v1.0', NOW()),
(4, 9, 2, 6, 'v1.0', NOW()),
(5, 6, NULL, NULL, 'v1.0', NOW()),
(6, 4, 5, 7, 'v1.0', NOW()),
(7, 6, NULL, 8, 'v1.0', NOW()),
(8, 7, NULL, NULL, 'v1.0', NOW()),
(9, NULL, 4, 13, 'v1.0', NOW()),
(10, 11, NULL, NULL, 'v1.0', NOW()),
(11, 13, 10, 12, 'v1.0', NOW()),
(12, 11, NULL, NULL, 'v1.0', NOW()),
(13, 9, 11, 15, 'v1.0', NOW()),
(14, 15, NULL, NULL, 'v1.0', NOW()),
(15, 13, 14, 16, 'v1.0', NOW()),
(16, 15, NULL, 17, 'v1.0', NOW()),
(17, 16, NULL, NULL, 'v1.0', NOW())
ON DUPLICATE KEY UPDATE
    parent_user_id = VALUES(parent_user_id),
    left_child_id = VALUES(left_child_id),
    right_child_id = VALUES(right_child_id);

-- --------------------------------------------------------------------
-- STEP 2: Table `graph_adjacency_records`
-- Purpose: Store the Adjacency List Representation of the project graph,
-- showing active dependency edges.
-- --------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS graph_adjacency_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    prerequisite_task_id INT NOT NULL,
    dependent_task_id INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_graph_edge UNIQUE (prerequisite_task_id, dependent_task_id),
    CONSTRAINT fk_gar_prereq FOREIGN KEY (prerequisite_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_gar_dependent FOREIGN KEY (dependent_task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Populate `graph_adjacency_records` from active dependency edges in `task_dependencies`
INSERT INTO graph_adjacency_records (prerequisite_task_id, dependent_task_id, created_at)
SELECT prerequisite_id, dependent_id, created_at
FROM task_dependencies
ON DUPLICATE KEY UPDATE
    created_at = VALUES(created_at);

