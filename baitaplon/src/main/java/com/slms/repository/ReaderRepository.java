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
 * Quản lý thông tin bạn đọc của thư viện.
 *
 * @author SLMS Team
 * @version 1.0
 */
public class ReaderRepository {

    // ==================== Câu lệnh SQL ====================
    private static final String SQL_FIND_ALL =
            "SELECT reader_id, full_name, reader_type, expiry_date, balance_fine, status FROM READERS ORDER BY full_name";

    private static final String SQL_FIND_BY_ID =
            "SELECT reader_id, full_name, reader_type, expiry_date, balance_fine, status FROM READERS WHERE reader_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO READERS (reader_id, full_name, reader_type, expiry_date, balance_fine, status) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE READERS SET full_name = ?, reader_type = ?, expiry_date = ?, balance_fine = ?, status = ? WHERE reader_id = ?";

    private static final String SQL_UPDATE_BALANCE =
            "UPDATE READERS SET balance_fine = ? WHERE reader_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM READERS WHERE reader_id = ?";

    /**
     * Lấy danh sách tất cả bạn đọc, sắp xếp theo họ tên.
     *
     * @return danh sách tất cả bạn đọc
     */
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

    /**
     * Tìm bạn đọc theo mã định danh.
     *
     * @param readerId mã bạn đọc cần tìm
     * @return {@link Optional} chứa bạn đọc nếu tìm thấy
     */
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

    /**
     * Thêm bạn đọc mới vào cơ sở dữ liệu.
     *
     * @param reader đối tượng bạn đọc cần thêm
     * @return đối tượng bạn đọc đã được lưu
     */
    public Reader save(Reader reader) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, reader.getReaderId());
            ps.setString(2, reader.getFullName());
            ps.setString(3, reader.getType().name());
            // Chuyển đổi LocalDate sang java.sql.Date
            ps.setDate(4, java.sql.Date.valueOf(reader.getExpiryDate()));
            ps.setDouble(5, reader.getBalanceFine());
            ps.setString(6, reader.getStatus());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thêm bạn đọc [" + reader.getReaderId() + "]: " + e.getMessage(), e);
        }
        return reader;
    }

    /**
     * Cập nhật toàn bộ thông tin bạn đọc.
     *
     * @param reader đối tượng bạn đọc chứa thông tin cập nhật
     * @return {@code true} nếu cập nhật thành công
     */
    public boolean update(Reader reader) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, reader.getFullName());
            ps.setString(2, reader.getType().name());
            ps.setDate(3, java.sql.Date.valueOf(reader.getExpiryDate()));
            ps.setDouble(4, reader.getBalanceFine());
            ps.setString(5, reader.getStatus());
            ps.setString(6, reader.getReaderId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật bạn đọc [" + reader.getReaderId() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật số dư tiền phạt của bạn đọc.
     *
     * @param readerId   mã bạn đọc
     * @param newBalance số dư tiền phạt mới
     * @return {@code true} nếu cập nhật thành công
     */
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

    /**
     * Xóa bạn đọc theo mã định danh.
     *
     * @param readerId mã bạn đọc cần xóa
     * @return {@code true} nếu xóa thành công
     */
    public boolean delete(String readerId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setString(1, readerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa bạn đọc [" + readerId + "]: " + e.getMessage(), e);
        }
    }

    // ==================== Phương thức hỗ trợ ====================

    /**
     * Chuyển đổi một dòng ResultSet thành đối tượng Reader.
     *
     * @param rs ResultSet đang trỏ đến dòng cần chuyển đổi
     * @return đối tượng Reader được ánh xạ từ ResultSet
     * @throws SQLException nếu có lỗi khi đọc dữ liệu
     */
    private Reader mapResultSetToReader(ResultSet rs) throws SQLException {
        Reader reader = new Reader();
        reader.setReaderId(rs.getString("reader_id"));
        reader.setFullName(rs.getString("full_name"));

        // Chuyển đổi chuỗi sang enum ReaderType
        reader.setType(ReaderType.valueOf(rs.getString("reader_type")));

        // Chuyển đổi java.sql.Date sang LocalDate
        java.sql.Date sqlDate = rs.getDate("expiry_date");
        if (sqlDate != null) {
            reader.setExpiryDate(sqlDate.toLocalDate());
        }

        reader.setBalanceFine(rs.getDouble("balance_fine"));
        reader.setStatus(rs.getString("status"));
        return reader;
    }
}
