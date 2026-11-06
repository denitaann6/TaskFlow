package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.controller.UserTreeController;
import com.taskflow.taskflow_backend.dto.UserDTO;
import com.taskflow.taskflow_backend.entity.User;
import com.taskflow.taskflow_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserTreeControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    private UserBSTService userBSTService;
    private UserTreeController controller;

    private User adminEntity;
    private User memberEntity;
    private UserDTO adminDTO;
    private UserDTO memberDTO;

    @BeforeEach
    void setUp() {
        userBSTService = new UserBSTService();
        controller = new UserTreeController(userRepository, authService, userBSTService);

        adminEntity = new User("admin", "hash", "System Administrator", "ADMIN", true, "Administrator");
        adminEntity.setId(1);

        memberEntity = new User("sheena", "hash", "Sheena", "MEMBER", true, "Sheena");
        memberEntity.setId(4);

        adminDTO = new UserDTO(1, "admin", "System Administrator", "ADMIN", true, "Administrator");
        memberDTO = new UserDTO(4, "sheena", "Sheena", "MEMBER", true, "Sheena");
    }

    @Test
    @DisplayName("ADMIN user can successfully fetch full User BST")
    void testAdminGetTreeSuccess() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminEntity);
        when(userRepository.findAll()).thenReturn(List.of(adminEntity, memberEntity));
        when(authService.toDTO(adminEntity)).thenReturn(adminDTO);
        when(authService.toDTO(memberEntity)).thenReturn(memberDTO);

        ResponseEntity<Map<String, Object>> response = controller.getTree(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().get("totalUsers"));
        assertNotNull(response.getBody().get("root"));
    }

    @Test
    @DisplayName("ADMIN user can successfully fetch Inorder traversal")
    void testAdminGetInorderSuccess() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminEntity);
        when(userRepository.findAll()).thenReturn(List.of(memberEntity, adminEntity)); // out of order
        when(authService.toDTO(adminEntity)).thenReturn(adminDTO);
        when(authService.toDTO(memberEntity)).thenReturn(memberDTO);

        ResponseEntity<List<UserDTO>> response = controller.getInorder(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<UserDTO> list = response.getBody();
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).getId()); // Inorder must sort by ID (1 then 4)
        assertEquals(4, list.get(1).getId());
    }

    @Test
    @DisplayName("ADMIN user can search User BST with valid trace")
    void testAdminSearchSuccess() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminEntity);
        when(userRepository.findAll()).thenReturn(List.of(adminEntity, memberEntity));
        when(authService.toDTO(adminEntity)).thenReturn(adminDTO);
        when(authService.toDTO(memberEntity)).thenReturn(memberDTO);

        ResponseEntity<Map<String, Object>> response = controller.searchUserBST(4, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("found"));
        assertEquals(2, body.get("comparisons"));
    }

    @Test
    @DisplayName("MEMBER request to User Tree API is rejected with 403 Forbidden")
    void testMemberForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator privileges required."))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.getTree(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Administrator privileges required"));
        verify(userRepository, never()).findAll();
    }

    @Test
    @DisplayName("Unauthenticated request to User Tree API is rejected with 401 Unauthorized")
    void testUnauthenticatedUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required. Please log in."))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.getInorder(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(userRepository, never()).findAll();
    }
}

