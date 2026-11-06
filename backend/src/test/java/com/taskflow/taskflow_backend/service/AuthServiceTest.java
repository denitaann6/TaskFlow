package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.dto.LoginRequest;
import com.taskflow.taskflow_backend.dto.LoginResponse;
import com.taskflow.taskflow_backend.entity.User;
import com.taskflow.taskflow_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Successful Admin login returns valid token and ADMIN role")
    void testSuccessfulAdminLogin() {
        String rawPass = "Admin@123";
        String hash = passwordEncoder.encode(rawPass);
        User admin = new User("admin", hash, "System Administrator", "ADMIN", true, "Administrator");
        admin.setId(1);

        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));

        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginResponse response = authService.login(new LoginRequest("admin", rawPass), request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getToken());
        assertEquals("ADMIN", response.getUser().getRole());
        assertEquals("admin", response.getUser().getUsername());
    }

    @Test
    @DisplayName("Successful Member login returns valid token and MEMBER role")
    void testSuccessfulMemberLogin() {
        String rawPass = "Member@123";
        String hash = passwordEncoder.encode(rawPass);
        User member = new User("john", hash, "John", "MEMBER", true, "John");
        member.setId(2);

        when(userRepository.findByUsernameIgnoreCase("john")).thenReturn(Optional.of(member));

        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginResponse response = authService.login(new LoginRequest("john", rawPass), request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getToken());
        assertEquals("MEMBER", response.getUser().getRole());
        assertEquals("john", response.getUser().getUsername());
    }

    @Test
    @DisplayName("Invalid password throws HTTP 401 Unauthorized")
    void testInvalidPasswordThrowsUnauthorized() {
        String hash = passwordEncoder.encode("CorrectPassword");
        User user = new User("admin", hash, "Administrator", "ADMIN", true, "Administrator");

        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authService.login(new LoginRequest("admin", "WrongPassword"), request));

        assertEquals(401, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Invalid username or password"));
    }

    @Test
    @DisplayName("Unknown username throws HTTP 401 Unauthorized")
    void testUnknownUserThrowsUnauthorized() {
        when(userRepository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authService.login(new LoginRequest("ghost", "pass"), request));

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Deactivated account throws HTTP 403 Forbidden")
    void testInactiveUserThrowsForbidden() {
        String rawPass = "Pass@123";
        String hash = passwordEncoder.encode(rawPass);
        User inactive = new User("deactivated", hash, "Inactive User", "MEMBER", false, "Inactive");

        when(userRepository.findByUsernameIgnoreCase("deactivated")).thenReturn(Optional.of(inactive));

        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authService.login(new LoginRequest("deactivated", rawPass), request));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("inactive"));
    }

    @Test
    @DisplayName("Logout invalidates session and revokes authentication token")
    void testLogoutInvalidatesSession() {
        String rawPass = "Admin@123";
        String hash = passwordEncoder.encode(rawPass);
        User admin = new User("admin", hash, "Administrator", "ADMIN", true, "Administrator");
        admin.setId(1);

        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));

        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginResponse response = authService.login(new LoginRequest("admin", rawPass), request);

        request.addHeader("Authorization", "Bearer " + response.getToken());
        assertTrue(authService.getCurrentUser(request).isPresent());

        // Perform logout
        authService.logout(request);

        // After logout with cleared session and invalidated token
        MockHttpServletRequest postLogoutRequest = new MockHttpServletRequest();
        postLogoutRequest.addHeader("Authorization", "Bearer " + response.getToken());
        assertTrue(authService.getCurrentUser(postLogoutRequest).isEmpty());
    }

    @Test
    @DisplayName("requireAdmin rejects Member with HTTP 403 Forbidden")
    void testRequireAdminRejectsMember() {
        User member = new User("john", "hash", "John", "MEMBER", true, "John");
        member.setId(5);

        when(userRepository.findById(5)).thenReturn(Optional.of(member));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(AuthService.SESSION_USER_KEY, authService.toDTO(member));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authService.requireAdmin(request));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Administrator privileges required"));
    }
}
