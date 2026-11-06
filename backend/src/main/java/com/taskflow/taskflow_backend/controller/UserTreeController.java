package com.taskflow.taskflow_backend.controller;

import com.taskflow.taskflow_backend.dto.UserDTO;
import com.taskflow.taskflow_backend.repository.UserRepository;
import com.taskflow.taskflow_backend.service.AuthService;
import com.taskflow.taskflow_backend.service.UserBSTService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dedicated REST Controller for Phase 2.1: User Binary Search Tree.
 * Restricts all tree structure and traversal operations strictly to authenticated Administrators.
 */
@RestController
@RequestMapping("/api/admin/users/tree")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.OPTIONS})
public class UserTreeController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final UserBSTService userBSTService;

    public UserTreeController(UserRepository userRepository,
                              AuthService authService,
                              UserBSTService userBSTService) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.userBSTService = userBSTService;
    }

    private List<UserDTO> loadCurrentUsers() {
        return userRepository.findAll().stream()
                .map(authService::toDTO)
                .toList();
    }

    /**
     * Get the full User BST root node and structural metadata.
     * Accessible to ADMIN only.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTree(HttpServletRequest httpRequest) {
        authService.requireAdmin(httpRequest);
        List<UserDTO> users = loadCurrentUsers();
        Map<String, Object> summary = userBSTService.getTreeSummary(users);
        return ResponseEntity.ok(summary);
    }

    /**
     * Inorder Traversal (Left -> Root -> Right).
     * Yields users strictly sorted in ascending order of User ID.
     * Accessible to ADMIN only.
     */
    @GetMapping("/inorder")
    public ResponseEntity<List<UserDTO>> getInorder(HttpServletRequest httpRequest) {
        authService.requireAdmin(httpRequest);
        List<UserDTO> users = loadCurrentUsers();
        List<UserDTO> inorder = userBSTService.getInorderTraversal(users);
        return ResponseEntity.ok(inorder);
    }

    /**
     * Preorder Traversal (Root -> Left -> Right).
     * Visits parent before child branches.
     * Accessible to ADMIN only.
     */
    @GetMapping("/preorder")
    public ResponseEntity<List<UserDTO>> getPreorder(HttpServletRequest httpRequest) {
        authService.requireAdmin(httpRequest);
        List<UserDTO> users = loadCurrentUsers();
        List<UserDTO> preorder = userBSTService.getPreorderTraversal(users);
        return ResponseEntity.ok(preorder);
    }

    /**
     * Postorder Traversal (Left -> Right -> Root).
     * Visits subtrees before parent nodes.
     * Accessible to ADMIN only.
     */
    @GetMapping("/postorder")
    public ResponseEntity<List<UserDTO>> getPostorder(HttpServletRequest httpRequest) {
        authService.requireAdmin(httpRequest);
        List<UserDTO> users = loadCurrentUsers();
        List<UserDTO> postorder = userBSTService.getPostorderTraversal(users);
        return ResponseEntity.ok(postorder);
    }

    /**
     * Search the BST by User ID with step trace and comparison count.
     * Accessible to ADMIN only.
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUserBST(
            @RequestParam(required = false) Integer id,
            HttpServletRequest httpRequest) {
        authService.requireAdmin(httpRequest);
        List<UserDTO> users = loadCurrentUsers();
        Map<String, Object> result = userBSTService.searchWithTrace(users, id);
        return ResponseEntity.ok(result);
    }
}

