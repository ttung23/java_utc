package com.slms.repository;

import com.slms.model.Reader;
import com.slms.model.enums.ReaderType;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thao tác CRUD đối với bảng READERS trong cơ sở dữ liệu.
 */
public class ReaderRepository {

    private static final String SQL_FIND_ALL =
            "SELECT reader_id, full_name, reader_type, expiry_date, balance_fine, status, email, phone, password_hash "
                    + "FROM READERS ORDER BY full_name";

    private static final String SQL_FIND_BY_ID =
            "SELECT reader_id, full_name, reader_type, expiry_date, balance_fine, status, email, phone, password_hash "
                    + "FROM READERS WHERE reader_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO READERS (reader_id, full_name, reader_type, expiry_date, balance_fine, status, email, phone, password_hash) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE READERS SET full_name = ?, reader_type = ?, expiry_date = ?, balance_fine = ?, status = ?, "
                    + "email = ?, phone = ? WHERE reader_id = ?";

    private static final String SQL_UPDATE_PASSWORD =
            "UPDATE READERS SET password_hash = ? WHERE reader_id = ?";

    private static final String SQL_UPDATE_BALANCE =
            "UPDATE READERS SET balance_fine = ? WHERE reader_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM READERS WHERE reader_id = ?";

    private static final String SQL_FIND_MISSING_PASSWORD =
            "SELECT reader_id FROM READERS WHERE password_hash IS NULL OR password_hash = ''";

    public List<Reader> findAll() {
        List<Reader> readers = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                readers.add(mapResultSetToReader(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách bạn đọc: " + e.getMessage(), e);
        }
        return readers;
    }

    public Optional<Reader> findById(String readerId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setString(1, readerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToReader(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm bạn đọc theo ID [" + readerId + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Reader save(Reader reader) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, reader.getReaderId());
            ps.setString(2, reader.getFullName());
            ps.setString(3, reader.getType().name());
            ps.setDate(4, java.sql.Date.valueOf(reader.getExpiryDate()));
            ps.setDouble(5, reader.getBalanceFine());
            ps.setString(6, reader.getStatus());
            ps.setString(7, reader.getEmail());
            ps.setString(8, reader.getPhone());
            ps.setString(9, reader.getPasswordHash());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thêm bạn đọc [" + reader.getReaderId() + "]: " + e.getMessage(), e);
        }
        return reader;
    }

    public boolean update(Reader reader) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, reader.getFullName());
            ps.setString(2, reader.getType().name());
            ps.setDate(3, java.sql.Date.valueOf(reader.getExpiryDate()));
            ps.setDouble(4, reader.getBalanceFine());
            ps.setString(5, reader.getStatus());
            ps.setString(6, reader.getEmail());
            ps.setString(7, reader.getPhone());
            ps.setString(8, reader.getReaderId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật bạn đọc [" + reader.getReaderId() + "]: " + e.getMessage(), e);
        }
    }

    public boolean updatePassword(String readerId, String passwordHash) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {
            ps.setString(1, passwordHash);
            ps.setString(2, readerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật mật khẩu độc giả: " + e.getMessage(), e);
        }
    }

    public boolean updateBalanceFine(String readerId, double newBalance) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_BALANCE)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, readerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật tiền phạt bạn đọc [" + readerId + "]: " + e.getMessage(), e);
        }
    }

    public boolean delete(String readerId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setString(1, readerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa bạn đọc [" + readerId + "]: " + e.getMessage(), e);
        }
    }

    public List<String> findIdsMissingPassword() {
        List<String> ids = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_MISSING_PASSWORD);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getString("reader_id"));
            }
        } catch (SQLException e) {
            // Cột chưa tồn tại trong quá trình migrate
        }
        return ids;
    }

    private Reader mapResultSetToReader(ResultSet rs) throws SQLException {
        Reader reader = new Reader();
        reader.setReaderId(rs.getString("reader_id"));
        reader.setFullName(rs.getString("full_name"));
        reader.setType(ReaderType.valueOf(rs.getString("reader_type")));
        java.sql.Date sqlDate = rs.getDate("expiry_date");
        if (sqlDate != null) {
            reader.setExpiryDate(sqlDate.toLocalDate());
        }
        reader.setBalanceFine(rs.getDouble("balance_fine"));
        reader.setStatus(rs.getString("status"));
        try {
            reader.setEmail(rs.getString("email"));
            reader.setPhone(rs.getString("phone"));
            reader.setPasswordHash(rs.getString("password_hash"));
        } catch (SQLException ignored) {
            // Schema cũ chưa có cột
        }
        return reader;
    }
}
