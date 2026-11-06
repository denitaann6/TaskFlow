package com.taskflow.taskflow_backend.controller;

import com.taskflow.taskflow_backend.dto.LoginRequest;
import com.taskflow.taskflow_backend.dto.LoginResponse;
import com.taskflow.taskflow_backend.dto.UserDTO;
import com.taskflow.taskflow_backend.entity.User;
import com.taskflow.taskflow_backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully logged out"
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(HttpServletRequest httpRequest) {
        User user = authService.requireUser(httpRequest);
        return ResponseEntity.ok(authService.toDTO(user));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers(HttpServletRequest httpRequest) {
        authService.requireAdmin(httpRequest);
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/team")
    public ResponseEntity<List<UserDTO>> getTeamMembers(HttpServletRequest httpRequest) {
        // Authenticated users (Admin and Member) can view active team members for collaboration
        authService.requireUser(httpRequest);
        List<UserDTO> team = authService.getAllUsers().stream()
                .filter(u -> !"ADMIN".equalsIgnoreCase(u.getRole()))
                .toList();
        return ResponseEntity.ok(team);
    }
}
