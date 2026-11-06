package com.taskflow.taskflow_backend.dto;

public class UserDTO {
    private Integer id;
    private String username;
    private String displayName;
    private String role;
    private boolean active;
    private String assignedMemberName;

    public UserDTO() {
    }

    public UserDTO(Integer id, String username, String displayName, String role, boolean active, String assignedMemberName) {
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
}
