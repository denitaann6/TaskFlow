package com.taskflow.taskflow_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data Transfer Object representing a node in the User Binary Search Tree.
 * Strictly excludes sensitive authentication fields (passwords, password hashes, tokens).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserTreeNodeDTO {

    private Integer id;
    private String username;
    private String displayName;
    private String role;
    private boolean active = true;
    private String assignedMemberName;

    private UserTreeNodeDTO left;
    private UserTreeNodeDTO right;

    public UserTreeNodeDTO() {
    }

    public UserTreeNodeDTO(UserDTO user) {
        if (user != null) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.displayName = user.getDisplayName();
            this.role = user.getRole();
            this.active = user.isActive();
            this.assignedMemberName = user.getAssignedMemberName();
        }
    }

    public UserTreeNodeDTO(Integer id, String username, String displayName, String role, boolean active, String assignedMemberName) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.active = active;
        this.assignedMemberName = assignedMemberName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getAssignedMemberName() {
        return assignedMemberName;
    }

    public void setAssignedMemberName(String assignedMemberName) {
        this.assignedMemberName = assignedMemberName;
    }

    public UserTreeNodeDTO getLeft() {
        return left;
    }

    public void setLeft(UserTreeNodeDTO left) {
        this.left = left;
    }

    public UserTreeNodeDTO getRight() {
        return right;
    }

    public void setRight(UserTreeNodeDTO right) {
        this.right = right;
    }
}

