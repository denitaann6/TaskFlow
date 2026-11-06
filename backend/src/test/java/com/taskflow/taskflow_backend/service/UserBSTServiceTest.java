package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.dto.UserDTO;
import com.taskflow.taskflow_backend.dto.UserTreeNodeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserBSTServiceTest {

    private UserBSTService bstService;
    private List<UserDTO> sampleUsers;

    @BeforeEach
    void setUp() {
        bstService = new UserBSTService();
        sampleUsers = new ArrayList<>();

        // Structured BST order: 8 (root), 4 (left), 12 (right), 2, 6, 10, 14
        sampleUsers.add(createUser(8, "amrith", "Amrith", "MEMBER"));
        sampleUsers.add(createUser(4, "sheena", "Sheena", "MEMBER"));
        sampleUsers.add(createUser(12, "member_a", "Member A", "MEMBER"));
        sampleUsers.add(createUser(2, "john", "John", "MEMBER"));
        sampleUsers.add(createUser(6, "mary", "Mary", "MEMBER"));
        sampleUsers.add(createUser(10, "usera", "User A", "MEMBER"));
        sampleUsers.add(createUser(14, "arjun", "Arjun", "MEMBER"));
    }

    private UserDTO createUser(int id, String username, String displayName, String role) {
        return new UserDTO(id, username, displayName, role, true, displayName);
    }

    @Test
    @DisplayName("Empty user list returns null root and empty traversals")
    void testEmptyUserList() {
        assertNull(bstService.buildTree(null));
        assertNull(bstService.buildTree(List.of()));

        assertTrue(bstService.getInorderTraversal(null).isEmpty());
        assertTrue(bstService.getPreorderTraversal(List.of()).isEmpty());
        assertTrue(bstService.getPostorderTraversal(null).isEmpty());

        Map<String, Object> searchEmpty = bstService.searchWithTrace(List.of(), 5);
        assertFalse((Boolean) searchEmpty.get("found"));
        assertEquals(0, searchEmpty.get("comparisons"));
    }

    @Test
    @DisplayName("Single-node tree correctly sets root with no children")
    void testSingleNodeTree() {
        List<UserDTO> single = List.of(createUser(1, "admin", "Admin", "ADMIN"));
        UserTreeNodeDTO root = bstService.buildTree(single);

        assertNotNull(root);
        assertEquals(1, root.getId());
        assertEquals("admin", root.getUsername());
        assertNull(root.getLeft());
        assertNull(root.getRight());

        List<UserDTO> inorder = bstService.getInorderTraversal(single);
        assertEquals(1, inorder.size());
        assertEquals(1, inorder.get(0).getId());
    }

    @Test
    @DisplayName("Inorder traversal strictly returns User IDs in ascending order")
    void testInorderTraversalAscendingOrder() {
        List<UserDTO> inorder = bstService.getInorderTraversal(sampleUsers);
        assertEquals(7, inorder.size());

        assertEquals(2, inorder.get(0).getId());
        assertEquals(4, inorder.get(1).getId());
        assertEquals(6, inorder.get(2).getId());
        assertEquals(8, inorder.get(3).getId());
        assertEquals(10, inorder.get(4).getId());
        assertEquals(12, inorder.get(5).getId());
        assertEquals(14, inorder.get(6).getId());
    }

    @Test
    @DisplayName("Preorder traversal follows Root -> Left -> Right sequence")
    void testPreorderTraversalOrder() {
        List<UserDTO> preorder = bstService.getPreorderTraversal(sampleUsers);
        assertEquals(7, preorder.size());

        // Root=8, LeftSubtree=(4, 2, 6), RightSubtree=(12, 10, 14)
        assertEquals(8, preorder.get(0).getId());
        assertEquals(4, preorder.get(1).getId());
        assertEquals(2, preorder.get(2).getId());
        assertEquals(6, preorder.get(3).getId());
        assertEquals(12, preorder.get(4).getId());
        assertEquals(10, preorder.get(5).getId());
        assertEquals(14, preorder.get(6).getId());
    }

    @Test
    @DisplayName("Postorder traversal follows Left -> Right -> Root sequence")
    void testPostorderTraversalOrder() {
        List<UserDTO> postorder = bstService.getPostorderTraversal(sampleUsers);
        assertEquals(7, postorder.size());

        // Left Leaves=(2, 6, 4), Right Leaves=(10, 14, 12), Root=8
        assertEquals(2, postorder.get(0).getId());
        assertEquals(6, postorder.get(1).getId());
        assertEquals(4, postorder.get(2).getId());
        assertEquals(10, postorder.get(3).getId());
        assertEquals(14, postorder.get(4).getId());
        assertEquals(12, postorder.get(5).getId());
        assertEquals(8, postorder.get(6).getId());
    }

    @Test
    @DisplayName("Successful BST search returns user details, correct comparisons, and decision path")
    void testSearchExistingUser() {
        // Search for User ID 6 (Path: 8 -> went LEFT -> 4 -> went RIGHT -> 6, 3 comparisons)
        Map<String, Object> result = bstService.searchWithTrace(sampleUsers, 6);

        assertTrue((Boolean) result.get("found"));
        assertEquals(3, result.get("comparisons"));

        UserDTO user = (UserDTO) result.get("user");
        assertNotNull(user);
        assertEquals(6, user.getId());
        assertEquals("mary", user.getUsername());

        @SuppressWarnings("unchecked")
        List<String> path = (List<String>) result.get("decisionPath");
        assertEquals(3, path.size());
        assertTrue(path.get(0).contains("branching LEFT"));
        assertTrue(path.get(1).contains("branching RIGHT"));
        assertTrue(path.get(2).contains("match found"));
    }

    @Test
    @DisplayName("Search for nonexistent User ID reports false and details leaf path")
    void testSearchNonExistentUser() {
        // Search for User ID 99 (Path: 8 -> RIGHT -> 12 -> RIGHT -> 14 -> RIGHT -> null, 3 comparisons)
        Map<String, Object> result = bstService.searchWithTrace(sampleUsers, 99);

        assertFalse((Boolean) result.get("found"));
        assertNull(result.get("user"));
        assertEquals(3, result.get("comparisons"));

        @SuppressWarnings("unchecked")
        List<String> path = (List<String>) result.get("decisionPath");
        assertEquals(4, path.size()); // 3 node visits + 1 terminal null notice
        assertTrue(path.get(path.size() - 1).contains("Reached null pointer"));
    }

    @Test
    @DisplayName("Invalid or null search ID handled safely without throwing exceptions")
    void testSearchInvalidId() {
        Map<String, Object> resNull = bstService.searchWithTrace(sampleUsers, null);
        assertFalse((Boolean) resNull.get("found"));
        assertEquals(0, resNull.get("comparisons"));

        Map<String, Object> resNeg = bstService.searchWithTrace(sampleUsers, -5);
        assertFalse((Boolean) resNeg.get("found"));
        assertEquals(0, resNeg.get("comparisons"));
    }

    @Test
    @DisplayName("Duplicate User ID insertion is handled idempotently without corrupting tree")
    void testDuplicateIdIgnored() {
        sampleUsers.add(createUser(8, "amrith_dup", "Amrith Duplicate", "MEMBER"));
        List<UserDTO> inorder = bstService.getInorderTraversal(sampleUsers);
        assertEquals(7, inorder.size()); // Duplicate 8 was ignored
    }

    @Test
    @DisplayName("Tree summary correctly computes height and leaf counts")
    void testTreeSummaryMetrics() {
        Map<String, Object> summary = bstService.getTreeSummary(sampleUsers);
        assertEquals(7, summary.get("totalUsers"));
        assertEquals(3, summary.get("height"));
        assertEquals(4, summary.get("leafCount")); // leaves: 2, 6, 10, 14
        assertNotNull(summary.get("root"));
    }

    @Test
    @DisplayName("UserTreeNodeDTO strictly excludes passwords and password hashes")
    void testNoSensitiveFieldsInTreeNode() {
        UserTreeNodeDTO node = new UserTreeNodeDTO(createUser(1, "admin", "Admin", "ADMIN"));
        // Assert UserTreeNodeDTO class has no getPassword or getPasswordHash methods
        assertDoesNotThrow(() -> {
            node.getClass().getDeclaredField("id");
            node.getClass().getDeclaredField("username");
            node.getClass().getDeclaredField("displayName");
            node.getClass().getDeclaredField("role");
        });
        assertThrows(NoSuchFieldException.class, () -> node.getClass().getDeclaredField("passwordHash"));
        assertThrows(NoSuchFieldException.class, () -> node.getClass().getDeclaredField("password"));
    }

    @Test
    @DisplayName("17 sequential users construct a balanced BST with root 9 and height 5")
    void testSeventeenUsersBalancedBST() {
        List<UserDTO> seventeen = new ArrayList<>();
        for (int i = 1; i <= 17; i++) {
            seventeen.add(createUser(i, "user" + i, "User " + i, "MEMBER"));
        }
        Map<String, Object> summary = bstService.getTreeSummary(seventeen);
        assertEquals(17, summary.get("totalUsers"));
        assertEquals(5, summary.get("height"), "Height must be 5 for 17 balanced users");
        assertEquals(8, summary.get("leafCount"), "Leaf count must be 8 for 17 balanced users");

        UserTreeNodeDTO root = (UserTreeNodeDTO) summary.get("root");
        assertNotNull(root);
        assertEquals(9, root.getId(), "Root must be User ID 9");
        assertNotNull(root.getLeft());
        assertEquals(4, root.getLeft().getId(), "Left child of root must be User ID 4");
        assertNotNull(root.getRight());
        assertEquals(13, root.getRight().getId(), "Right child of root must be User ID 13");

        // Verify Inorder traversal (Strictly ascending 1..17)
        List<UserDTO> inorder = bstService.getInorderTraversal(seventeen);
        assertEquals(17, inorder.size());
        for (int i = 0; i < 17; i++) {
            assertEquals(i + 1, inorder.get(i).getId());
        }

        // Verify Preorder traversal (Root -> Left -> Right)
        List<UserDTO> preorder = bstService.getPreorderTraversal(seventeen);
        assertEquals(17, preorder.size());
        List<Integer> expectedPreorder = List.of(9, 4, 2, 1, 3, 6, 5, 7, 8, 13, 11, 10, 12, 15, 14, 16, 17);
        for (int i = 0; i < 17; i++) {
            assertEquals(expectedPreorder.get(i), preorder.get(i).getId());
        }

        // Verify Postorder traversal (Left -> Right -> Root)
        List<UserDTO> postorder = bstService.getPostorderTraversal(seventeen);
        assertEquals(17, postorder.size());
        List<Integer> expectedPostorder = List.of(1, 3, 2, 5, 8, 7, 6, 4, 10, 12, 11, 14, 17, 16, 15, 13, 9);
        for (int i = 0; i < 17; i++) {
            assertEquals(expectedPostorder.get(i), postorder.get(i).getId());
        }

        // Verify Search for existing user (ID 4: 2 comparisons)
        Map<String, Object> s4 = bstService.searchWithTrace(seventeen, 4);
        assertTrue((Boolean) s4.get("found"));
        assertEquals(2, s4.get("comparisons"));
        assertNotNull(s4.get("user"));
        assertEquals(4, ((UserDTO) s4.get("user")).getId());

        // Verify Search for nonexistent user (ID 99: 5 comparisons)
        Map<String, Object> s99 = bstService.searchWithTrace(seventeen, 99);
        assertFalse((Boolean) s99.get("found"));
        assertEquals(5, s99.get("comparisons"));
        assertNull(s99.get("user"));
    }
}

