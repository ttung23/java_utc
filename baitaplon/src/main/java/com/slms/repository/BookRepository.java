package com.slms.repository;

import com.slms.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thao tác CRUD đối với bảng BOOKS trong cơ sở dữ liệu.
 * Sử dụng PreparedStatement để phòng chống SQL Injection.
 *
 * @author SLMS Team
 * @version 1.0
 */
public class BookRepository {

    // ==================== Câu lệnh SQL ====================
    private static final String SQL_FIND_ALL =
            "SELECT isbn, title, author, publisher, publish_year, category FROM BOOKS ORDER BY title";

    private static final String SQL_FIND_BY_ISBN =
            "SELECT isbn, title, author, publisher, publish_year, category FROM BOOKS WHERE isbn = ?";

    private static final String SQL_INSERT =
            "INSERT INTO BOOKS (isbn, title, author, publisher, publish_year, category) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE BOOKS SET title = ?, author = ?, publisher = ?, publish_year = ?, category = ? WHERE isbn = ?";

    private static final String SQL_DELETE =
            "DELETE FROM BOOKS WHERE isbn = ?";

    private static final String SQL_SEARCH_BY_TITLE =
            "SELECT isbn, title, author, publisher, publish_year, category FROM BOOKS WHERE LOWER(title) LIKE ?";

    /**
     * Lấy danh sách tất cả sách, sắp xếp theo tên.
     *
     * @return danh sách tất cả các cuốn sách
     */
    public List<Book> findAll() {
        List<Book> books = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                books.add(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách sách: " + e.getMessage(), e);
        }
        return books;
    }

    /**
     * Tìm sách theo mã ISBN.
     *
     * @param isbn mã ISBN của sách cần tìm
     * @return {@link Optional} chứa sách nếu tìm thấy, rỗng nếu không
     */
    public Optional<Book> findByIsbn(String isbn) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ISBN)) {

            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBook(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm sách theo ISBN [" + isbn + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Thêm sách mới vào cơ sở dữ liệu.
     *
     * @param book đối tượng sách cần thêm
     * @return đối tượng sách đã được lưu
     */
    public Book save(Book book) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getPublisher());
            ps.setInt(5, book.getPublishYear());
            ps.setString(6, book.getCategory());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thêm sách [" + book.getIsbn() + "]: " + e.getMessage(), e);
        }
        return book;
    }

    /**
     * Cập nhật thông tin sách theo ISBN.
     *
     * @param book đối tượng sách chứa thông tin cập nhật
     * @return {@code true} nếu cập nhật thành công, {@code false} nếu không tìm thấy
     */
    public boolean update(Book book) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getPublisher());
            ps.setInt(4, book.getPublishYear());
            ps.setString(5, book.getCategory());
            ps.setString(6, book.getIsbn());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật sách [" + book.getIsbn() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa sách theo mã ISBN.
     *
     * @param isbn mã ISBN của sách cần xóa
     * @return {@code true} nếu xóa thành công, {@code false} nếu không tìm thấy
     */
    public boolean delete(String isbn) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setString(1, isbn);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa sách [" + isbn + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Tìm kiếm sách theo từ khóa trong tiêu đề (không phân biệt hoa thường).
     *
     * @param keyword từ khóa tìm kiếm
     * @return danh sách sách có tiêu đề chứa từ khóa
     */
    public List<Book> searchByTitle(String keyword) {
        List<Book> books = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SEARCH_BY_TITLE)) {

            // Thêm ký tự '%' để tìm kiếm gần đúng
            ps.setString(1, "%" + keyword.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    books.add(mapResultSetToBook(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm kiếm sách theo tiêu đề [" + keyword + "]: " + e.getMessage(), e);
        }
        return books;
    }

    // ==================== Phương thức hỗ trợ ====================

    /**
     * Chuyển đổi một dòng ResultSet thành đối tượng Book.
     *
     * @param rs ResultSet đang trỏ đến dòng cần chuyển đổi
     * @return đối tượng Book được ánh xạ từ ResultSet
     * @throws SQLException nếu có lỗi khi đọc dữ liệu
     */
    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setIsbn(rs.getString("isbn"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setPublisher(rs.getString("publisher"));
        book.setPublishYear(rs.getInt("publish_year"));
        book.setCategory(rs.getString("category"));
        return book;
    }
}
