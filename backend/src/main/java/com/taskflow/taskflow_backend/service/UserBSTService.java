package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.dto.UserDTO;
import com.taskflow.taskflow_backend.dto.UserTreeNodeDTO;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service implementing a Balanced Binary Search Tree (BST)
 * to organize system users by their unique User ID.
 * Constructs a height-balanced tree by recursively selecting the median element.
 * Supports O(log n) search, tree metric computation,
 * and standard tree traversals (Inorder, Preorder, Postorder).
 */
@Service
public class UserBSTService {

    /**
     * Builds a balanced BST from a list of user DTOs.
     * Deduplicates by User ID, sorts in ascending order, and constructs
     * the tree recursively by selecting the middle user as the root of each subtree.
     */
    public UserTreeNodeDTO buildTree(List<UserDTO> users) {
        if (users == null || users.isEmpty()) {
            return null;
        }

        // Filter valid IDs and deduplicate by User ID (preserving first occurrence)
        Map<Integer, UserDTO> uniqueUsers = new LinkedHashMap<>();
        for (UserDTO user : users) {
            if (user != null && user.getId() != null) {
                uniqueUsers.putIfAbsent(user.getId(), user);
            }
        }
        if (uniqueUsers.isEmpty()) {
            return null;
        }

        List<UserDTO> sortedUsers = new ArrayList<>(uniqueUsers.values());
        sortedUsers.sort(Comparator.comparing(UserDTO::getId));

        return buildBalancedBST(sortedUsers, 0, sortedUsers.size() - 1);
    }

    /**
     * Recursively constructs a height-balanced BST from a sorted user list.
     * Picks the middle element as root of each subtree to maintain minimal height O(log n).
     *
     * Left child User ID < Parent User ID.
     * Right child User ID > Parent User ID.
     */
    private UserTreeNodeDTO buildBalancedBST(List<UserDTO> sortedUsers, int start, int end) {
        if (start > end) {
            return null;
        }
        int mid = start + (end - start) / 2;
        UserDTO midUser = sortedUsers.get(mid);
        UserTreeNodeDTO node = new UserTreeNodeDTO(midUser);

        node.setLeft(buildBalancedBST(sortedUsers, start, mid - 1));
        node.setRight(buildBalancedBST(sortedUsers, mid + 1, end));

        return node;
    }

    /**
     * Inserts a user into the BST based on their unique User ID.
     * Retained for standard dynamic BST insertion operations.
     */
    public UserTreeNodeDTO insert(UserTreeNodeDTO root, UserDTO user) {
        if (user == null || user.getId() == null) {
            return root;
        }

        if (root == null) {
            return new UserTreeNodeDTO(user);
        }

        if (user.getId() < root.getId()) {
            root.setLeft(insert(root.getLeft(), user));
        } else if (user.getId() > root.getId()) {
            root.setRight(insert(root.getRight(), user));
        }
        // Duplicate IDs are ignored to maintain BST uniqueness invariants
        return root;
    }

    /**
     * Inorder Traversal (Left -> Root -> Right).
     * Yields users strictly sorted in ascending order of User ID.
     */
    public List<UserDTO> getInorderTraversal(List<UserDTO> users) {
        UserTreeNodeDTO root = buildTree(users);
        List<UserDTO> result = new ArrayList<>();
        inorder(root, result);
        return result;
    }

    private void inorder(UserTreeNodeDTO node, List<UserDTO> list) {
        if (node != null) {
            inorder(node.getLeft(), list);
            list.add(toDTO(node));
            inorder(node.getRight(), list);
        }
    }

    /**
     * Preorder Traversal (Root -> Left -> Right).
     * Visits parent before child branches, capturing tree structural hierarchy.
     */
    public List<UserDTO> getPreorderTraversal(List<UserDTO> users) {
        UserTreeNodeDTO root = buildTree(users);
        List<UserDTO> result = new ArrayList<>();
        preorder(root, result);
        return result;
    }

    private void preorder(UserTreeNodeDTO node, List<UserDTO> list) {
        if (node != null) {
            list.add(toDTO(node));
            preorder(node.getLeft(), list);
            preorder(node.getRight(), list);
        }
    }

    /**
     * Postorder Traversal (Left -> Right -> Root).
     * Visits subtrees bottom-up before parent nodes.
     */
    public List<UserDTO> getPostorderTraversal(List<UserDTO> users) {
        UserTreeNodeDTO root = buildTree(users);
        List<UserDTO> result = new ArrayList<>();
        postorder(root, result);
        return result;
    }

    private void postorder(UserTreeNodeDTO node, List<UserDTO> list) {
        if (node != null) {
            postorder(node.getLeft(), list);
            postorder(node.getRight(), list);
            list.add(toDTO(node));
        }
    }

    /**
     * Searches the BST for a user with the specified User ID.
     * Returns match status, user details, total comparisons, and step-by-step decision trace.
     */
    public Map<String, Object> searchWithTrace(List<UserDTO> users, Integer targetId) {
        Map<String, Object> response = new LinkedHashMap<>();
        List<String> path = new ArrayList<>();

        if (targetId == null || targetId <= 0) {
            response.put("found", false);
            response.put("targetId", targetId);
            response.put("user", null);
            response.put("comparisons", 0);
            response.put("message", "Please enter a valid positive User ID to search.");
            response.put("decisionPath", path);
            return response;
        }

        UserTreeNodeDTO root = buildTree(users);
        if (root == null) {
            response.put("found", false);
            response.put("targetId", targetId);
            response.put("user", null);
            response.put("comparisons", 0);
            response.put("message", "The User BST is currently empty.");
            response.put("decisionPath", List.of("Root is null -> No users in tree."));
            return response;
        }

        UserTreeNodeDTO current = root;
        int comparisons = 0;
        UserDTO foundUser = null;

        while (current != null) {
            comparisons++;
            int currentId = current.getId();
            String name = current.getDisplayName() != null ? current.getDisplayName() : current.getUsername();

            if (targetId.equals(currentId)) {
                path.add(String.format("Step %d: Visited Node #%d (\"%s\") -> Key match found! (Target %d == %d)",
                        comparisons, currentId, name, targetId, currentId));
                foundUser = toDTO(current);
                break;
            } else if (targetId < currentId) {
                path.add(String.format("Step %d: Visited Node #%d (\"%s\") -> Target %d < %d, branching LEFT",
                        comparisons, currentId, name, targetId, currentId));
                current = current.getLeft();
            } else {
                path.add(String.format("Step %d: Visited Node #%d (\"%s\") -> Target %d > %d, branching RIGHT",
                        comparisons, currentId, name, targetId, currentId));
                current = current.getRight();
            }
        }

        if (foundUser != null) {
            response.put("found", true);
            response.put("targetId", targetId);
            response.put("user", foundUser);
            response.put("comparisons", comparisons);
            response.put("message", String.format("User #%d (\"%s\") found in BST after %d comparison(s).",
                    foundUser.getId(), foundUser.getDisplayName(), comparisons));
        } else {
            path.add(String.format("Step %d: Reached null pointer -> User ID #%d does not exist in the BST.",
                    comparisons + 1, targetId));
            response.put("found", false);
            response.put("targetId", targetId);
            response.put("user", null);
            response.put("comparisons", comparisons);
            response.put("message", String.format("User ID #%d was not found in the BST (%d comparisons evaluated).",
                    targetId, comparisons));
        }

        response.put("decisionPath", path);
        return response;
    }

    /**
     * Generates a comprehensive summary payload including tree root, height, and node counts.
     */
    public Map<String, Object> getTreeSummary(List<UserDTO> users) {
        Map<String, Object> summary = new LinkedHashMap<>();
        int count = users != null ? users.size() : 0;
        UserTreeNodeDTO root = buildTree(users);

        summary.put("totalUsers", count);
        summary.put("height", calculateHeight(root));
        summary.put("leafCount", countLeaves(root));
        summary.put("root", root);
        return summary;
    }

    private int calculateHeight(UserTreeNodeDTO node) {
        if (node == null) return 0;
        return 1 + Math.max(calculateHeight(node.getLeft()), calculateHeight(node.getRight()));
    }

    private int countLeaves(UserTreeNodeDTO node) {
        if (node == null) return 0;
        if (node.getLeft() == null && node.getRight() == null) return 1;
        return countLeaves(node.getLeft()) + countLeaves(node.getRight());
    }

    private UserDTO toDTO(UserTreeNodeDTO node) {
        if (node == null) return null;
        return new UserDTO(
                node.getId(),
                node.getUsername(),
                node.getDisplayName(),
                node.getRole(),
                node.isActive(),
                node.getAssignedMemberName()
        );
    }
}

