package com.slms.service;

import com.slms.model.Book;
import com.slms.model.BookCopy;
import com.slms.model.enums.CopyStatus;
import com.slms.repository.BookRepository;
import com.slms.repository.BookCopyRepository;
import com.slms.repository.LoanRepository;

import java.time.LocalDate;
import java.util.ArrayList;
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
    private final LoanRepository loanRepository;
    private final com.slms.repository.CategoryRepository categoryRepository;

    /**
     * Khởi tạo BookService với các repository mặc định.
     */
    public BookService() {
        this.bookRepository = new BookRepository();
        this.bookCopyRepository = new BookCopyRepository();
        this.loanRepository = new LoanRepository();
        this.categoryRepository = new com.slms.repository.CategoryRepository();
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
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()
                || book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sách và tác giả không được để trống.");
        }

        // Kiểm tra sách đã tồn tại chưa
        Optional<Book> existing = bookRepository.findByIsbn(book.getIsbn());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Sách với ISBN '" + book.getIsbn() + "' đã tồn tại trong hệ thống.");
        }

        syncBookCategory(book);
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
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()
                || book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sách và tác giả không được để trống.");
        }

        // Kiểm tra sách có tồn tại không
        Optional<Book> existing = bookRepository.findByIsbn(book.getIsbn());
        if (existing.isEmpty()) {
            return false;
        }

        syncBookCategory(book);
        return bookRepository.update(book);
    }

    private void syncBookCategory(Book book) {
        if (book == null) return;
        if (book.getCategoryId() != null && !book.getCategoryId().trim().isEmpty()) {
            categoryRepository.findById(book.getCategoryId().trim()).ifPresent(c -> {
                book.setCategoryId(c.getCategoryId());
                book.setCategory(c.getCategoryName());
            });
        } else if (book.getCategory() != null && !book.getCategory().trim().isEmpty()) {
            com.slms.model.Category c = categoryRepository.getOrCreateByName(book.getCategory().trim());
            book.setCategoryId(c.getCategoryId());
            book.setCategory(c.getCategoryName());
        }
    }

    /**
     * Xóa sách khỏi hệ thống theo ISBN.
     * Chỉ cho phép xóa khi đầu sách không còn bản sao vật lý.
     *
     * @param isbn mã ISBN của sách cần xóa
     * @return true nếu xóa thành công, false nếu không tìm thấy sách
     * @throws IllegalStateException nếu sách vẫn còn bản sao vật lý
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

        // Giữ nguyên đầu sách còn bản sao vật lý hoặc lịch sử lưu thông.
        List<BookCopy> copies = bookCopyRepository.findByIsbn(isbn);
        if (!copies.isEmpty()) {
            throw new IllegalStateException("Hãy xóa các bản sao không có lịch sử mượn trước khi xóa đầu sách.");
        }

        return bookRepository.delete(isbn);
    }

    /**
     * Tìm kiếm sách theo từ khóa trong tiêu đề hoặc tác giả.
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
     * Lấy danh sách tất cả bản sao sách trong toàn hệ thống thư viện.
     *
     * @return danh sách tất cả bản sao sách
     */
    public List<BookCopy> getAllCopies() {
        return bookCopyRepository.findAll();
    }

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
     * Thêm hàng loạt bản sao cho một cuốn sách với số lượng tùy chỉnh.
     * Tự động sinh mã bản sao (Copy ID) và mã vạch (Barcode) tiếp theo nếu không nhập.
     *
     * @param isbn mã ISBN của sách
     * @param price giá bìa
     * @param quantity số lượng bản sao cần tạo
     * @param startCopyId mã bản sao bắt đầu (tùy chọn)
     * @param startBarcode mã vạch bắt đầu (tùy chọn)
     * @return danh sách các bản sao đã được tạo thành công
     */
    public synchronized List<BookCopy> addCopies(String isbn, double price, int quantity, String startCopyId, String startBarcode) {
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã ISBN không được để trống.");
        }
        Optional<Book> book = bookRepository.findByIsbn(isbn);
        if (book.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy sách với ISBN '" + isbn + "'.");
        }
        if (quantity < 1) {
            quantity = 1;
        }

        List<BookCopy> allCopies = bookCopyRepository.findAll();
        int maxCopyNum = 0;
        int maxBarcodeNum = 0;
        for (BookCopy c : allCopies) {
            int cNum = extractTrailingNumber(c.getCopyId());
            if (cNum > maxCopyNum) maxCopyNum = cNum;
            int bNum = extractTrailingNumber(c.getBarcode());
            if (bNum > maxBarcodeNum) maxBarcodeNum = bNum;
        }

        String copyPrefix = "CP";
        int copyPadding = 3;
        int nextCopyNum = maxCopyNum + 1;
        if (startCopyId != null && !startCopyId.trim().isEmpty()) {
            String trimmed = startCopyId.trim();
            int parsed = extractTrailingNumber(trimmed);
            if (parsed > 0) {
                copyPrefix = extractPrefix(trimmed);
                copyPadding = Math.max(extractPadding(trimmed), 1);
                nextCopyNum = parsed;
            } else {
                copyPrefix = trimmed;
                nextCopyNum = maxCopyNum + 1;
            }
        }

        String barcodePrefix = "BC-";
        int barcodePadding = 5;
        int nextBarcodeNum = maxBarcodeNum + 1;
        if (startBarcode != null && !startBarcode.trim().isEmpty()) {
            String trimmed = startBarcode.trim();
            int parsed = extractTrailingNumber(trimmed);
            if (parsed > 0) {
                barcodePrefix = extractPrefix(trimmed);
                barcodePadding = Math.max(extractPadding(trimmed), 1);
                nextBarcodeNum = parsed;
            } else {
                barcodePrefix = trimmed;
                nextBarcodeNum = maxBarcodeNum + 1;
            }
        }

        List<BookCopy> added = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < quantity; i++) {
            String cId = String.format("%s%0" + copyPadding + "d", copyPrefix, nextCopyNum);
            while (bookCopyRepository.findById(cId).isPresent()) {
                nextCopyNum++;
                cId = String.format("%s%0" + copyPadding + "d", copyPrefix, nextCopyNum);
            }

            String bCode = String.format("%s%0" + barcodePadding + "d", barcodePrefix, nextBarcodeNum);
            while (bookCopyRepository.findByBarcode(bCode).isPresent()) {
                nextBarcodeNum++;
                bCode = String.format("%s%0" + barcodePadding + "d", barcodePrefix, nextBarcodeNum);
            }

            BookCopy copy = new BookCopy();
            copy.setCopyId(cId);
            copy.setIsbn(isbn);
            copy.setBarcode(bCode);
            copy.setAcquisitionDate(today);
            copy.setPrice(price);
            copy.setStatus(CopyStatus.Available);

            bookCopyRepository.save(copy);
            added.add(copy);

            nextCopyNum++;
            nextBarcodeNum++;
        }

        return added;
    }

    private int extractTrailingNumber(String str) {
        if (str == null || str.isEmpty()) return 0;
        int i = str.length() - 1;
        while (i >= 0 && Character.isDigit(str.charAt(i))) {
            i--;
        }
        if (i == str.length() - 1) return 0;
        try {
            return Integer.parseInt(str.substring(i + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String extractPrefix(String str) {
        if (str == null || str.isEmpty()) return "";
        int i = str.length() - 1;
        while (i >= 0 && Character.isDigit(str.charAt(i))) {
            i--;
        }
        return str.substring(0, i + 1);
    }

    private int extractPadding(String str) {
        if (str == null || str.isEmpty()) return 0;
        int i = str.length() - 1;
        while (i >= 0 && Character.isDigit(str.charAt(i))) {
            i--;
        }
        return str.length() - 1 - i;
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

    public boolean deleteCopy(String copyId) {
        if (copyId == null || copyId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã bản sao không được để trống.");
        }

        Optional<BookCopy> existing = bookCopyRepository.findById(copyId);
        if (existing.isEmpty()) {
            return false;
        }
        if (existing.get().getStatus() == CopyStatus.Loaned
                || existing.get().getStatus() == CopyStatus.Reserved) {
            throw new IllegalStateException("Không thể xóa bản sao đang được mượn hoặc đặt trước.");
        }
        if (loanRepository.hasLoansByCopyId(copyId)) {
            throw new IllegalStateException("Không thể xóa bản sao đã có lịch sử mượn.");
        }
        return bookCopyRepository.delete(copyId);
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

    /**
     * Lấy thông tin số lượng bản sao khả dụng / tổng số bản sao của một đầu sách.
     *
     * @param isbn mã ISBN
     * @return map chứa isbn, availableCount, totalCopies
     */
    public java.util.Map<String, Object> getAvailability(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new IllegalArgumentException("ISBN không được để trống.");
        }
        String trimmed = isbn.trim();
        List<BookCopy> copies = bookCopyRepository.findByIsbn(trimmed);
        int available = bookCopyRepository.countAvailableByIsbn(trimmed);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("isbn", trimmed);
        result.put("availableCount", available);
        result.put("totalCopies", copies.size());
        return result;
    }
}
