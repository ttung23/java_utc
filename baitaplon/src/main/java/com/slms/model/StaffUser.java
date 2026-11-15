package com.slms.model;

import com.slms.model.enums.UserRole;

/**
 * Tài khoản nhân viên thư viện (thủ thư / admin).
 */
public class StaffUser {

    private String staffId;
    private String username;
    private String fullName;
    private String passwordHash;
    private UserRole role;
    private String status;

    public StaffUser() {}

    public StaffUser(String staffId, String username, String fullName, String passwordHash, UserRole role, String status) {
        this.staffId = staffId;
        this.username = username;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
