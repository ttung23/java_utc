package com.slms.repository;

import com.slms.model.Shelf;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thao tác CRUD đối với bảng SHELVES trong cơ sở dữ liệu.
 */
public class ShelfRepository {

    private static final String SQL_FIND_ALL =
            "SELECT s.shelf_id, s.shelf_name, s.description, COUNT(b.isbn) AS book_count " +
            "FROM SHELVES s LEFT JOIN BOOKS b ON s.shelf_id = b.shelf_id " +
            "GROUP BY s.shelf_id, s.shelf_name, s.description ORDER BY s.shelf_name";

    private static final String SQL_FIND_BY_ID =
            "SELECT shelf_id, shelf_name, description FROM SHELVES WHERE shelf_id = ?";

    private static final String SQL_FIND_BY_NAME =
            "SELECT shelf_id, shelf_name, description FROM SHELVES WHERE LOWER(TRIM(shelf_name)) = LOWER(TRIM(?))";

    private static final String SQL_INSERT =
            "INSERT INTO SHELVES (shelf_id, shelf_name, description) VALUES (?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE SHELVES SET shelf_name = ?, description = ? WHERE shelf_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM SHELVES WHERE shelf_id = ?";

    /**
     * Lấy toàn bộ danh sách kệ sách kèm số lượng đầu sách đặt tại kệ.
     */
    public List<Shelf> findAll() {
        List<Shelf> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách kệ sách: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Tìm kệ sách theo mã shelf_id.
     */
    public Optional<Shelf> findById(String shelfId) {
        if (shelfId == null || shelfId.trim().isEmpty()) return Optional.empty();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setString(1, shelfId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm kệ sách theo ID [" + shelfId + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Tìm kệ theo tên (không phân biệt hoa thường).
     */
    public Optional<Shelf> findByName(String shelfName) {
        if (shelfName == null || shelfName.trim().isEmpty()) return Optional.empty();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NAME)) {
            ps.setString(1, shelfName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm kệ sách theo tên [" + shelfName + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Lưu kệ sách mới vào cơ sở dữ liệu.
     */
    public synchronized Shelf save(Shelf shelf) {
        if (shelf == null) throw new IllegalArgumentException("Thông tin kệ không được null.");
        if (shelf.getShelfName() == null || shelf.getShelfName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên kệ không được để trống.");
        }

        if (shelf.getShelfId() == null || shelf.getShelfId().trim().isEmpty()) {
            shelf.setShelfId(generateNextShelfId());
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, shelf.getShelfId());
            ps.setString(2, shelf.getShelfName().trim());
            ps.setString(3, shelf.getDescription());
            ps.executeUpdate();
            return shelf;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thêm kệ sách [" + shelf.getShelfName() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy kệ theo tên, nếu chưa có thì tự động tạo mới.
     */
    public synchronized Shelf getOrCreateByName(String shelfName) {
        if (shelfName == null || shelfName.trim().isEmpty()) {
            shelfName = "Kệ A1 - Tầng 1";
        }
        Optional<Shelf> opt = findByName(shelfName);
        if (opt.isPresent()) {
            return opt.get();
        }
        Shelf newShelf = new Shelf();
        newShelf.setShelfName(shelfName.trim());
        return save(newShelf);
    }

    /**
     * Sinh mã kệ tiếp theo: SH001, SH002, SH003...
     */
    private String generateNextShelfId() {
        String sql = "SELECT shelf_id FROM SHELVES WHERE shelf_id LIKE 'SH%'";
        int maxNum = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("shelf_id");
                if (id != null && id.startsWith("SH")) {
                    try {
                        int num = Integer.parseInt(id.substring(2).trim());
                        if (num > maxNum) maxNum = num;
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (SQLException ignored) {}
        return String.format("SH%03d", maxNum + 1);
    }

    /**
     * Cập nhật thông tin kệ sách.
     */
    public boolean update(Shelf shelf) {
        if (shelf == null || shelf.getShelfId() == null) return false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, shelf.getShelfName().trim());
            ps.setString(2, shelf.getDescription());
            ps.setString(3, shelf.getShelfId().trim());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                // Đồng bộ tên vị trí sang bảng BOOKS
                try (PreparedStatement ps2 = conn.prepareStatement(
                        "UPDATE BOOKS SET location = ? WHERE shelf_id = ?")) {
                    ps2.setString(1, shelf.getShelfName().trim());
                    ps2.setString(2, shelf.getShelfId().trim());
                    ps2.executeUpdate();
                } catch (SQLException ignored) {}
            }
            return ok;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật kệ sách [" + shelf.getShelfId() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa kệ sách (nếu không có đầu sách nào liên kết).
     */
    public boolean delete(String shelfId) {
        if (shelfId == null || shelfId.trim().isEmpty()) return false;
        int count = countBooksByShelfId(shelfId.trim());
        if (count > 0) {
            throw new IllegalStateException("Không thể xóa kệ vì đang có " + count + " đầu sách đặt tại đây. Hãy chuyển các sách này sang kệ khác trước!");
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setString(1, shelfId.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa kệ sách [" + shelfId + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Đếm số đầu sách đang đặt tại kệ này.
     */
    public int countBooksByShelfId(String shelfId) {
        String sql = "SELECT COUNT(*) FROM BOOKS WHERE shelf_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shelfId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignored) {}
        return 0;
    }

    private Shelf mapResultSet(ResultSet rs) throws SQLException {
        Shelf s = new Shelf();
        s.setShelfId(rs.getString("shelf_id"));
        s.setShelfName(rs.getString("shelf_name"));
        s.setDescription(rs.getString("description"));
        try {
            s.setBookCount(rs.getInt("book_count"));
        } catch (SQLException ignored) {}
        return s;
    }
}

