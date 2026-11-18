package com.slms.repository;

import com.slms.model.StaffUser;
import com.slms.model.enums.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Truy xuất tài khoản nhân viên (STAFF_USERS).
 */
public class StaffRepository {

    private static final String SQL_FIND_BY_USERNAME =
            "SELECT staff_id, username, full_name, password_hash, role, status, avatar FROM STAFF_USERS WHERE username = ?";

    private static final String SQL_FIND_BY_ID =
            "SELECT staff_id, username, full_name, password_hash, role, status, avatar FROM STAFF_USERS WHERE staff_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO STAFF_USERS (staff_id, username, full_name, password_hash, role, status, avatar) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_AVATAR =
            "UPDATE STAFF_USERS SET avatar = ? WHERE staff_id = ?";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM STAFF_USERS";

    public Optional<StaffUser> findByUsername(String username) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm staff theo username: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<StaffUser> findById(String staffId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setString(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm staff theo id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public StaffUser save(StaffUser staff) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, staff.getStaffId());
            ps.setString(2, staff.getUsername());
            ps.setString(3, staff.getFullName());
            ps.setString(4, staff.getPasswordHash());
            ps.setString(5, staff.getRole().name());
            ps.setString(6, staff.getStatus());
            ps.setString(7, staff.getAvatar());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu staff: " + e.getMessage(), e);
        }
        return staff;
    }

    public void updateAvatar(String staffId, String avatar) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_AVATAR)) {
            ps.setString(1, avatar);
            ps.setString(2, staffId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật avatar staff: " + e.getMessage(), e);
        }
    }

    public int count() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            return 0;
        }
        return 0;
    }

    private StaffUser map(ResultSet rs) throws SQLException {
        StaffUser s = new StaffUser();
        s.setStaffId(rs.getString("staff_id"));
        s.setUsername(rs.getString("username"));
        s.setFullName(rs.getString("full_name"));
        s.setPasswordHash(rs.getString("password_hash"));
        s.setRole(UserRole.valueOf(rs.getString("role")));
        s.setStatus(rs.getString("status"));
        s.setAvatar(rs.getString("avatar"));
        return s;
    }
}
