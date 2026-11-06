package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.dto.LoginRequest;
import com.taskflow.taskflow_backend.dto.LoginResponse;
import com.taskflow.taskflow_backend.dto.UserDTO;
import com.taskflow.taskflow_backend.entity.User;
import com.taskflow.taskflow_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    public static final String SESSION_USER_KEY = "TASKFLOW_AUTH_USER";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // In-memory token store: Token -> User ID (thread-safe)
    private final Map<String, Integer> tokenRegistry = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()
                || request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }

        String username = request.getUsername().trim();
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive. Contact the administrator.");
        }

        // Session fixation protection: change session ID if session already exists
        HttpSession session = httpRequest.getSession(true);
        if (!session.isNew()) {
            httpRequest.changeSessionId();
        }

        UserDTO userDTO = toDTO(user);
        session.setAttribute(SESSION_USER_KEY, userDTO);

        // Generate a secure session token
        String token = UUID.randomUUID().toString();
        tokenRegistry.put(token, user.getId());

        return new LoginResponse(true, "Login successful", token, userDTO);
    }

    public void logout(HttpServletRequest httpRequest) {
        // Invalidate HTTP session
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Remove token from registry
        String token = extractToken(httpRequest);
        if (token != null) {
            tokenRegistry.remove(token);
        }
    }

    public Optional<User> getCurrentUser(HttpServletRequest httpRequest) {
        // 1. Try session attribute
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            Object obj = session.getAttribute(SESSION_USER_KEY);
            if (obj instanceof UserDTO userDTO) {
                return userRepository.findById(userDTO.getId()).filter(User::isActive);
            }
        }

        // 2. Try token header
        String token = extractToken(httpRequest);
        if (token != null && tokenRegistry.containsKey(token)) {
            Integer userId = tokenRegistry.get(token);
            return userRepository.findById(userId).filter(User::isActive);
        }

        return Optional.empty();
    }

    public User requireUser(HttpServletRequest httpRequest) {
        return getCurrentUser(httpRequest)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required. Please log in."));
    }

    public User requireAdmin(HttpServletRequest httpRequest) {
        User user = requireUser(httpRequest);
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator privileges required.");
        }
        return user;
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.isActive(),
                user.getAssignedMemberName()
        );
    }

    private String extractToken(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        String xAuth = httpRequest.getHeader("X-Auth-Token");
        if (xAuth != null && !xAuth.trim().isEmpty()) {
            return xAuth.trim();
        }
        return null;
    }
}
