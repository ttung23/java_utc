package com.slms.service;

import com.slms.model.Book;
import com.slms.model.BookCopy;
import com.slms.model.enums.CopyStatus;
import com.slms.repository.BookRepository;
import com.slms.repository.BookCopyRepository;

import java.util.List;
import java.util.Optional;

/**
 * Lớp dịch vụ quản lý sách và bản sao sách.
 * Cung cấp các chức năng CRUD cho Book và BookCopy.
 *
 * @author SLMS Team
 */
public class BookService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;

    /**
     * Khởi tạo BookService với các repository mặc định.
     */
    public BookService() {
        this.bookRepository = new BookRepository();
        this.bookCopyRepository = new BookCopyRepository();
    }

    // ========================== QUẢN LÝ SÁCH ==========================

    /**
     * Lấy danh sách tất cả các sách trong hệ thống.
     *
     * @return danh sách tất cả sách
     */
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    /**
     * Tìm sách theo mã ISBN.
     *
     * @param isbn mã ISBN của sách cần tìm
     * @return Optional chứa sách nếu tìm thấy, rỗng nếu không
     */
    public Optional<Book> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    /**
     * Thêm sách mới vào hệ thống.
     *
     * @param book đối tượng sách cần thêm
     * @return đối tượng sách đã được lưu
     * @throws IllegalArgumentException nếu sách đã tồn tại hoặc dữ liệu không hợp lệ
     */
    public Book addBook(Book book) {
        // Kiểm tra dữ liệu đầu vào
        if (book == null || book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            throw new IllegalArgumentException("Thông tin sách không hợp lệ. ISBN không được để trống.");
        }

        // Kiểm tra sách đã tồn tại chưa
        Optional<Book> existing = bookRepository.findByIsbn(book.getIsbn());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Sách với ISBN '" + book.getIsbn() + "' đã tồn tại trong hệ thống.");
        }

        bookRepository.save(book);
        return book;
    }

    /**
     * Cập nhật thông tin sách.
     *
     * @param book đối tượng sách chứa thông tin cập nhật
     * @return true nếu cập nhật thành công, false nếu không tìm thấy sách
     * @throws IllegalArgumentException nếu dữ liệu không hợp lệ
     */
    public boolean updateBook(Book book) {
        if (book == null || book.getIsbn() == null || book.getIsbn().trim().isEmpty()) {
            throw new IllegalArgumentException("Thông tin sách không hợp lệ. ISBN không được để trống.");
        }

        // Kiểm tra sách có tồn tại không
        Optional<Book> existing = bookRepository.findByIsbn(book.getIsbn());
        if (existing.isEmpty()) {
            return false;
        }

        return bookRepository.update(book);
    }

    /**
     * Xóa sách khỏi hệ thống theo ISBN.
     * Chỉ cho phép xóa khi không còn bản sao nào đang được mượn.
     *
     * @param isbn mã ISBN của sách cần xóa
     * @return true nếu xóa thành công, false nếu không tìm thấy sách
     * @throws IllegalStateException nếu sách vẫn còn bản sao đang được mượn
     */
    public boolean deleteBook(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new IllegalArgumentException("ISBN không được để trống.");
        }

        // Kiểm tra sách có tồn tại không
        Optional<Book> existing = bookRepository.findByIsbn(isbn);
        if (existing.isEmpty()) {
            return false;
        }

        // Kiểm tra các bản sao - không cho xóa nếu có bản sao đang mượn
        List<BookCopy> copies = bookCopyRepository.findByIsbn(isbn);
        boolean hasLoanedCopies = copies.stream()
                .anyMatch(copy -> copy.getStatus() == CopyStatus.Loaned);
        if (hasLoanedCopies) {
            throw new IllegalStateException("Không thể xóa sách vì còn bản sao đang được mượn.");
        }

        return bookRepository.delete(isbn);
    }

    /**
     * Tìm kiếm sách theo từ khóa trong tiêu đề.
     *
     * @param keyword từ khóa tìm kiếm
     * @return danh sách sách phù hợp với từ khóa
     */
    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return bookRepository.findAll();
        }
        return bookRepository.searchByTitle(keyword.trim());
    }

    // ========================== QUẢN LÝ BẢN SAO SÁCH ==========================

    /**
     * Lấy danh sách tất cả bản sao của một cuốn sách theo ISBN.
     *
     * @param isbn mã ISBN của sách
     * @return danh sách bản sao của sách
     */
    public List<BookCopy> getCopiesByIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new IllegalArgumentException("ISBN không được để trống.");
        }
        return bookCopyRepository.findByIsbn(isbn);
    }

    /**
     * Thêm bản sao mới cho một cuốn sách.
     *
     * @param copy đối tượng bản sao cần thêm
     * @return đối tượng bản sao đã được lưu
     * @throws IllegalArgumentException nếu sách gốc không tồn tại hoặc dữ liệu không hợp lệ
     */
    public BookCopy addCopy(BookCopy copy) {
        if (copy == null || copy.getIsbn() == null || copy.getIsbn().trim().isEmpty()) {
            throw new IllegalArgumentException("Thông tin bản sao không hợp lệ.");
        }

        // Kiểm tra sách gốc có tồn tại không
        Optional<Book> book = bookRepository.findByIsbn(copy.getIsbn());
        if (book.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy sách với ISBN '" + copy.getIsbn() + "'.");
        }

        bookCopyRepository.save(copy);
        return copy;
    }

    /**
     * Cập nhật trạng thái của một bản sao sách.
     *
     * @param copyId mã bản sao
     * @param status trạng thái mới
     * @return true nếu cập nhật thành công, false nếu không tìm thấy bản sao
     */
    public boolean updateCopyStatus(String copyId, CopyStatus status) {
        if (copyId == null || copyId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã bản sao không được để trống.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Trạng thái mới không được để trống.");
        }

        // Kiểm tra bản sao có tồn tại không
        Optional<BookCopy> existing = bookCopyRepository.findById(copyId);
        if (existing.isEmpty()) {
            return false;
        }

        return bookCopyRepository.updateStatus(copyId, status);
    }

    /**
     * Đếm số bản sao có sẵn (trạng thái Available) của một cuốn sách.
     *
     * @param isbn mã ISBN của sách
     * @return số bản sao có sẵn
     */
    public int countAvailableCopies(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new IllegalArgumentException("ISBN không được để trống.");
        }
        return bookCopyRepository.countAvailableByIsbn(isbn);
    }
}
