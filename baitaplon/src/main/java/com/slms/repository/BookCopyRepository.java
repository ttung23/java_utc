package com.slms.repository;

import com.slms.model.BookCopy;
import com.slms.model.enums.CopyStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thao tác CRUD đối với bảng BOOK_COPIES trong cơ sở dữ liệu.
 * Mỗi bản sao (copy) thuộc về một cuốn sách (Book) thông qua ISBN.
 *
 * @author SLMS Team
 * @version 1.0
 */
public class BookCopyRepository {

    // ==================== Câu lệnh SQL ====================
    private static final String SQL_FIND_ALL =
            "SELECT copy_id, isbn, barcode, acquisition_date, price, status FROM BOOK_COPIES ORDER BY copy_id";

    private static final String SQL_FIND_BY_ISBN =
            "SELECT copy_id, isbn, barcode, acquisition_date, price, status FROM BOOK_COPIES WHERE isbn = ?";

    private static final String SQL_FIND_BY_ID =
            "SELECT copy_id, isbn, barcode, acquisition_date, price, status FROM BOOK_COPIES WHERE copy_id = ?";

    private static final String SQL_FIND_BY_BARCODE =
            "SELECT copy_id, isbn, barcode, acquisition_date, price, status FROM BOOK_COPIES WHERE barcode = ?";

    private static final String SQL_INSERT =
            "INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE BOOK_COPIES SET status = ? WHERE copy_id = ?";

    private static final String SQL_COUNT_AVAILABLE =
            "SELECT COUNT(*) FROM BOOK_COPIES WHERE isbn = ? AND status = 'Available'";

    /**
     * Lấy danh sách tất cả bản sao sách.
     *
     * @return danh sách tất cả bản sao
     */
    public List<BookCopy> findAll() {
        List<BookCopy> copies = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                copies.add(mapResultSetToBookCopy(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách bản sao sách: " + e.getMessage(), e);
        }
        return copies;
    }

    /**
     * Tìm tất cả bản sao theo mã ISBN.
     *
     * @param isbn mã ISBN của sách
     * @return danh sách bản sao thuộc cuốn sách đó
     */
    public List<BookCopy> findByIsbn(String isbn) {
        List<BookCopy> copies = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ISBN)) {

            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    copies.add(mapResultSetToBookCopy(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm bản sao theo ISBN [" + isbn + "]: " + e.getMessage(), e);
        }
        return copies;
    }

    /**
     * Tìm bản sao theo mã copy_id.
     *
     * @param copyId mã định danh bản sao
     * @return {@link Optional} chứa bản sao nếu tìm thấy
     */
    public Optional<BookCopy> findById(String copyId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setString(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBookCopy(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm bản sao theo ID [" + copyId + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Tìm bản sao theo mã barcode.
     * Đây là phương thức quan trọng dùng khi mượn/trả sách bằng quét mã vạch.
     *
     * @param barcode mã vạch của bản sao
     * @return {@link Optional} chứa bản sao nếu tìm thấy
     */
    public Optional<BookCopy> findByBarcode(String barcode) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_BARCODE)) {

            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBookCopy(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm bản sao theo barcode [" + barcode + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Thêm bản sao sách mới vào cơ sở dữ liệu.
     *
     * @param copy đối tượng bản sao cần thêm
     * @return đối tượng bản sao đã được lưu
     */
    public BookCopy save(BookCopy copy) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, copy.getCopyId());
            ps.setString(2, copy.getIsbn());
            ps.setString(3, copy.getBarcode());
            // Chuyển đổi LocalDate sang java.sql.Date
            ps.setDate(4, java.sql.Date.valueOf(copy.getAcquisitionDate()));
            ps.setDouble(5, copy.getPrice());
            ps.setString(6, copy.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thêm bản sao sách [" + copy.getCopyId() + "]: " + e.getMessage(), e);
        }
        return copy;
    }

    /**
     * Cập nhật trạng thái của bản sao sách.
     *
     * @param copyId mã định danh bản sao
     * @param status trạng thái mới
     * @return {@code true} nếu cập nhật thành công
     */
    public boolean updateStatus(String copyId, CopyStatus status) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {

            ps.setString(1, status.name());
            ps.setString(2, copyId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật trạng thái bản sao [" + copyId + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Đếm số bản sao có sẵn (Available) theo ISBN.
     *
     * @param isbn mã ISBN của sách
     * @return số lượng bản sao có trạng thái Available
     */
    public int countAvailableByIsbn(String isbn) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT_AVAILABLE)) {

            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi đếm bản sao có sẵn theo ISBN [" + isbn + "]: " + e.getMessage(), e);
        }
        return 0;
    }

    // ==================== Phương thức hỗ trợ ====================

    /**
     * Chuyển đổi một dòng ResultSet thành đối tượng BookCopy.
     *
     * @param rs ResultSet đang trỏ đến dòng cần chuyển đổi
     * @return đối tượng BookCopy được ánh xạ từ ResultSet
     * @throws SQLException nếu có lỗi khi đọc dữ liệu
     */
    private BookCopy mapResultSetToBookCopy(ResultSet rs) throws SQLException {
        BookCopy copy = new BookCopy();
        copy.setCopyId(rs.getString("copy_id"));
        copy.setIsbn(rs.getString("isbn"));
        copy.setBarcode(rs.getString("barcode"));

        // Chuyển đổi java.sql.Date sang LocalDate
        java.sql.Date sqlDate = rs.getDate("acquisition_date");
        if (sqlDate != null) {
            copy.setAcquisitionDate(sqlDate.toLocalDate());
        }

        copy.setPrice(rs.getDouble("price"));

        // Chuyển đổi chuỗi trạng thái sang enum CopyStatus
        copy.setStatus(CopyStatus.valueOf(rs.getString("status")));
        return copy;
    }
}
