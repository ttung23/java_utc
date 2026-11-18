package com.slms.repository;

import com.slms.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thao tác CRUD đối với bảng CATEGORIES trong cơ sở dữ liệu.
 */
public class CategoryRepository {

    private static final String SQL_FIND_ALL =
            "SELECT c.category_id, c.category_name, c.description, COUNT(b.isbn) AS book_count " +
            "FROM CATEGORIES c LEFT JOIN BOOKS b ON c.category_id = b.category_id " +
            "GROUP BY c.category_id, c.category_name, c.description ORDER BY c.category_name";

    private static final String SQL_FIND_BY_ID =
            "SELECT category_id, category_name, description FROM CATEGORIES WHERE category_id = ?";

    private static final String SQL_FIND_BY_NAME =
            "SELECT category_id, category_name, description FROM CATEGORIES WHERE LOWER(TRIM(category_name)) = LOWER(TRIM(?))";

    private static final String SQL_INSERT =
            "INSERT INTO CATEGORIES (category_id, category_name, description) VALUES (?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE CATEGORIES SET category_name = ?, description = ? WHERE category_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM CATEGORIES WHERE category_id = ?";

    /**
     * Lấy toàn bộ danh sách thể loại.
     */
    public List<Category> findAll() {
        List<Category> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách thể loại: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Tìm thể loại theo mã category_id.
     */
    public Optional<Category> findById(String categoryId) {
        if (categoryId == null || categoryId.trim().isEmpty()) return Optional.empty();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setString(1, categoryId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm thể loại theo ID [" + categoryId + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Tìm thể loại theo tên (không phân biệt hoa thường).
     */
    public Optional<Category> findByName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) return Optional.empty();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NAME)) {
            ps.setString(1, categoryName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm thể loại theo tên [" + categoryName + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Lưu thể loại mới vào cơ sở dữ liệu.
     */
    public synchronized Category save(Category category) {
        if (category == null) throw new IllegalArgumentException("Thông tin thể loại không được null.");
        if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thể loại không được để trống.");
        }

        if (category.getCategoryId() == null || category.getCategoryId().trim().isEmpty()) {
            category.setCategoryId(generateNextCategoryId());
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, category.getCategoryId());
            ps.setString(2, category.getCategoryName().trim());
            ps.setString(3, category.getDescription());
            ps.executeUpdate();
            return category;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thêm thể loại [" + category.getCategoryName() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy thể loại theo tên, nếu chưa có thì tự động tạo mới.
     */
    public synchronized Category getOrCreateByName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            categoryName = "General";
        }
        Optional<Category> opt = findByName(categoryName);
        if (opt.isPresent()) {
            return opt.get();
        }
        Category newCat = new Category();
        newCat.setCategoryName(categoryName.trim());
        return save(newCat);
    }

    /**
     * Sinh mã thể loại tiếp theo: CAT001, CAT002, CAT003...
     */
    private String generateNextCategoryId() {
        String sql = "SELECT category_id FROM CATEGORIES WHERE category_id LIKE 'CAT%'";
        int maxNum = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("category_id");
                if (id != null && id.startsWith("CAT")) {
                    try {
                        int num = Integer.parseInt(id.substring(3).trim());
                        if (num > maxNum) maxNum = num;
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (SQLException ignored) {}
        return String.format("CAT%03d", maxNum + 1);
    }

    /**
     * Cập nhật thông tin thể loại.
     */
    public boolean update(Category category) {
        if (category == null || category.getCategoryId() == null) return false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, category.getCategoryName().trim());
            ps.setString(2, category.getDescription());
            ps.setString(3, category.getCategoryId().trim());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                // Đồng bộ tên thể loại sang bảng BOOKS
                try (PreparedStatement ps2 = conn.prepareStatement(
                        "UPDATE BOOKS SET category = ? WHERE category_id = ?")) {
                    ps2.setString(1, category.getCategoryName().trim());
                    ps2.setString(2, category.getCategoryId().trim());
                    ps2.executeUpdate();
                } catch (SQLException ignored) {}
            }
            return ok;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật thể loại [" + category.getCategoryId() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa thể loại (nếu không có đầu sách nào liên kết).
     */
    public boolean delete(String categoryId) {
        if (categoryId == null || categoryId.trim().isEmpty()) return false;
        int count = countBooksByCategoryId(categoryId.trim());
        if (count > 0) {
            throw new IllegalStateException("Không thể xóa thể loại vì đang có " + count + " đầu sách liên kết. Hãy chuyển thể loại cho các sách này trước!");
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setString(1, categoryId.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa thể loại [" + categoryId + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Đếm số đầu sách đang thuộc về thể loại này.
     */
    public int countBooksByCategoryId(String categoryId) {
        String sql = "SELECT COUNT(*) FROM BOOKS WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoryId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignored) {}
        return 0;
    }

    private Category mapResultSet(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setCategoryId(rs.getString("category_id"));
        c.setCategoryName(rs.getString("category_name"));
        c.setDescription(rs.getString("description"));
        try {
            c.setBookCount(rs.getInt("book_count"));
        } catch (SQLException ignored) {}
        return c;
    }
}

