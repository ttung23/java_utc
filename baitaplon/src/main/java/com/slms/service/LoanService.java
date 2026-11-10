package com.slms.service;

import com.slms.model.*;
import com.slms.model.enums.*;
import com.slms.dto.LoanDTO;
import com.slms.dto.ReturnResultDTO;
import com.slms.exception.BookNotAvailableException;
import com.slms.exception.FineExceededException;
import com.slms.exception.MaxBorrowLimitExceededException;
import com.slms.exception.ReaderNotEligibleException;
import com.slms.repository.*;
import com.slms.util.IdGenerator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lớp dịch vụ xử lý nghiệp vụ mượn trả sách — LỚP QUAN TRỌNG NHẤT trong hệ thống.
 *
 * <p>Chức năng chính:</p>
 * <ul>
 *   <li><b>UC21 — Lập phiếu mượn sách:</b> Tạo phiếu mượn mới sau khi kiểm tra đầy đủ
 *       các điều kiện về tư cách độc giả và trạng thái bản sao sách</li>
 *   <li><b>UC22 — Tiếp nhận trả sách &amp; xử lý sự cố:</b> Xử lý trả sách bình thường,
 *       sách hư hỏng, sách mất; tính phạt quá hạn và tạo hóa đơn phạt tương ứng</li>
 * </ul>
 *
 * <p>Quy tắc nghiệp vụ:</p>
 * <ul>
 *   <li>Phạt quá hạn: 5.000 VNĐ/ngày/sách</li>
 *   <li>Phạt hư hỏng: 100-200% giá bản sao sách</li>
 *   <li>Phạt mất sách: 150% giá bản sao sách</li>
 *   <li>Ngưỡng nợ phạt tối đa cho phép mượn: 50.000 VNĐ</li>
 * </ul>
 *
 * @author SLMS Team
 */
public class LoanService {

    private final LoanRepository loanRepository;
    private final ReaderRepository readerRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final FineService fineService;
    private final ReservationRepository reservationRepository;

    /**
     * Khởi tạo LoanService với các repository và service mặc định.
     * FineService được inject qua constructor (không dùng Spring DI).
     */
    public LoanService() {
        this.loanRepository = new LoanRepository();
        this.readerRepository = new ReaderRepository();
        this.bookCopyRepository = new BookCopyRepository();
        this.bookRepository = new BookRepository();
        this.fineService = new FineService();
        this.reservationRepository = new ReservationRepository();
    }

    /**
     * Constructor hỗ trợ inject FineService từ bên ngoài (dùng cho testing hoặc tùy chỉnh).
     *
     * @param fineService dịch vụ quản lý tiền phạt
     */
    public LoanService(FineService fineService) {
        this.loanRepository = new LoanRepository();
        this.readerRepository = new ReaderRepository();
        this.bookCopyRepository = new BookCopyRepository();
        this.bookRepository = new BookRepository();
        this.fineService = fineService;
        this.reservationRepository = new ReservationRepository();
    }

    // =====================================================================================
    //  UC21: LẬP PHIẾU MƯỢN SÁCH
    // =====================================================================================

    /**
     * <b>UC21: Lập phiếu mượn sách</b>
     *
     * <p>Luồng xử lý chính:</p>
     * <ol>
     *   <li>Tìm độc giả theo mã — ném ReaderNotEligibleException nếu không tìm thấy</li>
     *   <li>Đếm số sách đang mượn (active loans) của độc giả</li>
     *   <li>Gọi reader.canBorrow(activeCount) — ném ngoại lệ tương ứng nếu không đủ điều kiện:
     *     <ul>
     *       <li>Trạng thái không ACTIVE → ReaderNotEligibleException("Thẻ độc giả không hợp lệ")</li>
     *       <li>Thẻ hết hạn → ReaderNotEligibleException("Thẻ thư viện đã hết hạn")</li>
     *       <li>Nợ phạt &gt; 50.000 VNĐ → FineExceededException("Nợ phạt vượt quá 50.000 VNĐ")</li>
     *       <li>Đã mượn tối đa → MaxBorrowLimitExceededException("Đã mượn tối đa số sách cho phép")</li>
     *     </ul>
     *   </li>
     *   <li>Tìm bản sao sách theo barcode — ném BookNotAvailableException nếu không tìm thấy</li>
     *   <li>Kiểm tra bản sao có sẵn (Available hoặc Reserved cho chính độc giả này)</li>
     *   <li>Tính ngày hết hạn = ngày mượn + số ngày mượn tối đa theo loại độc giả</li>
     *   <li>Tạo đối tượng Loan với trạng thái BORROWED</li>
     *   <li>Lưu phiếu mượn vào DB</li>
     *   <li>Cập nhật trạng thái bản sao sang Loaned</li>
     *   <li>Nếu bản sao đang Reserved và đúng độc giả đặt trước → cập nhật trạng thái đặt trước sang Fulfilled</li>
     *   <li>Trả về LoanDTO chứa đầy đủ thông tin</li>
     * </ol>
     *
     * @param readerId mã độc giả muốn mượn sách
     * @param barcode  mã vạch (barcode) của bản sao sách
     * @return LoanDTO chứa thông tin phiếu mượn đã tạo
     * @throws ReaderNotEligibleException     nếu độc giả không đủ tư cách mượn sách
     * @throws FineExceededException          nếu nợ phạt vượt quá 50.000 VNĐ
     * @throws MaxBorrowLimitExceededException nếu đã mượn tối đa số sách cho phép
     * @throws BookNotAvailableException       nếu bản sao sách không có sẵn
     */
    public LoanDTO createLoan(String readerId, String barcode) {
        // Kiểm tra dữ liệu đầu vào
        if (readerId == null || readerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã độc giả không được để trống.");
        }
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã vạch (barcode) không được để trống.");
        }

        Connection conn = null;
        try {
            // Lấy connection và bắt đầu transaction
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // ===== BƯỚC 1: Tìm độc giả =====
            Optional<Reader> readerOpt = readerRepository.findById(readerId);
            if (readerOpt.isEmpty()) {
                throw new ReaderNotEligibleException("Không tìm thấy độc giả với mã '" + readerId + "'.");
            }
            Reader reader = readerOpt.get();

            // ===== BƯỚC 2: Đếm số sách đang mượn =====
            int activeLoansCount = loanRepository.countActiveLoansByReaderId(readerId);

            // ===== BƯỚC 3: Kiểm tra tư cách mượn sách =====
            if (!reader.canBorrow(activeLoansCount)) {
                // Phân loại nguyên nhân cụ thể để ném đúng ngoại lệ
                if (!"ACTIVE".equalsIgnoreCase(reader.getStatus())) {
                    throw new ReaderNotEligibleException("Thẻ độc giả không hợp lệ");
                }
                if (reader.getExpiryDate() != null && reader.getExpiryDate().isBefore(LocalDate.now())) {
                    throw new ReaderNotEligibleException("Thẻ thư viện đã hết hạn");
                }
                if (reader.getBalanceFine() > 50000) {
                    throw new FineExceededException("Nợ phạt vượt quá 50.000 VNĐ");
                }
                throw new MaxBorrowLimitExceededException("Đã mượn tối đa số sách cho phép");
            }

            // ===== BƯỚC 4: Tìm bản sao sách theo barcode =====
            Optional<BookCopy> copyOpt = bookCopyRepository.findByBarcode(barcode);
            if (copyOpt.isEmpty()) {
                throw new BookNotAvailableException("Không tìm thấy bản sao sách với mã vạch '" + barcode + "'.");
            }
            BookCopy copy = copyOpt.get();

            // ===== BƯỚC 5: Kiểm tra trạng thái bản sao =====
            boolean isReservedForThisReader = false;
            Reservation matchedReservation = null;

            if (copy.getStatus() == CopyStatus.Reserved) {
                // Nếu sách đang được đặt trước, kiểm tra xem có phải cho độc giả này không
                List<Reservation> pendingReservations = reservationRepository.findPendingByIsbn(copy.getIsbn());
                for (Reservation res : pendingReservations) {
                    if (res.getReaderId().equals(readerId)) {
                        isReservedForThisReader = true;
                        matchedReservation = res;
                        break;
                    }
                }
                if (!isReservedForThisReader) {
                    throw new BookNotAvailableException(
                            "Bản sao sách đang được đặt trước cho độc giả khác.");
                }
            } else if (!copy.isAvailable()) {
                throw new BookNotAvailableException(
                        "Bản sao sách không có sẵn. Trạng thái hiện tại: " + copy.getStatus());
            }

            // ===== BƯỚC 6: Tính ngày hết hạn =====
            LocalDateTime borrowDate = LocalDateTime.now();
            int maxBorrowDays = reader.getType().getMaxBorrowDays();
            LocalDateTime dueDate = borrowDate.plusDays(maxBorrowDays);

            // ===== BƯỚC 7: Tạo đối tượng Loan =====
            String loanId = IdGenerator.generateLoanId();
            Loan loan = new Loan(loanId, readerId, copy.getCopyId(),
                    borrowDate, dueDate, null, LoanStatus.BORROWED);

            // ===== BƯỚC 8: Lưu phiếu mượn =====
            loanRepository.save(loan);

            // ===== BƯỚC 9: Cập nhật trạng thái bản sao sang Loaned =====
            bookCopyRepository.updateStatus(copy.getCopyId(), CopyStatus.Loaned);

            // ===== BƯỚC 10: Xử lý đặt trước (nếu có) =====
            if (isReservedForThisReader && matchedReservation != null) {
                reservationRepository.updateStatus(
                        matchedReservation.getReservationId(),
                        ReservationStatus.Fulfilled
                );
            }

            // Commit transaction
            conn.commit();

            // ===== BƯỚC 11: Tạo và trả về LoanDTO =====
            // Lấy thông tin sách để điền vào DTO
            String bookTitle = "";
            Optional<Book> bookOpt = bookRepository.findByIsbn(copy.getIsbn());
            if (bookOpt.isPresent()) {
                bookTitle = bookOpt.get().getTitle();
            }

            return new LoanDTO(
                    loanId,
                    readerId,
                    reader.getFullName(),
                    copy.getCopyId(),
                    barcode,
                    bookTitle,
                    borrowDate,
                    dueDate,
                    LoanStatus.BORROWED.name()
            );

        } catch (SQLException e) {
            // Rollback transaction khi có lỗi SQL
            rollbackQuietly(conn);
            throw new RuntimeException("Lỗi hệ thống khi tạo phiếu mượn: " + e.getMessage(), e);
        } catch (ReaderNotEligibleException | FineExceededException
                 | MaxBorrowLimitExceededException | BookNotAvailableException e) {
            // Rollback transaction khi có lỗi nghiệp vụ
            rollbackQuietly(conn);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Lỗi không xác định khi tạo phiếu mượn: " + e.getMessage(), e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    // =====================================================================================
    //  UC22: TIẾP NHẬN TRẢ SÁCH & XỬ LÝ SỰ CỐ
    // =====================================================================================

    /**
     * <b>UC22: Tiếp nhận trả sách &amp; xử lý sự cố</b>
     *
     * <p>Luồng xử lý chính:</p>
     * <ol>
     *   <li>Tìm bản sao sách theo barcode — ném ngoại lệ nếu không tìm thấy</li>
     *   <li>Tìm phiếu mượn đang hoạt động (active) cho bản sao này — ném ngoại lệ nếu không tìm thấy</li>
     *   <li>Ghi nhận ngày trả = ngày hiện tại</li>
     *   <li>Xử lý theo từng trường hợp:
     *     <ul>
     *       <li><b>Sách hư hỏng (isDamaged=true):</b>
     *         <ul>
     *           <li>Cập nhật trạng thái phiếu mượn → DAMAGED_RETURN</li>
     *           <li>Cập nhật trạng thái bản sao → Damaged</li>
     *           <li>Tạo phạt hư hỏng: damagePercent% giá bản sao (mặc định 100%, phạm vi 100-200%)</li>
     *         </ul>
     *       </li>
     *       <li><b>Sách mất (isLost=true):</b>
     *         <ul>
     *           <li>Cập nhật trạng thái phiếu mượn → LOST_RETURN</li>
     *           <li>Cập nhật trạng thái bản sao → Lost</li>
     *           <li>Tạo phạt mất sách: 150% giá bản sao</li>
     *         </ul>
     *       </li>
     *       <li><b>Trả bình thường:</b>
     *         <ul>
     *           <li>Tính số ngày quá hạn</li>
     *           <li>Nếu quá hạn: tạo phạt quá hạn (5.000 VNĐ × số ngày)</li>
     *           <li>Cập nhật trạng thái phiếu mượn → RETURNED hoặc OVERDUE</li>
     *           <li>Kiểm tra đơn đặt trước đang chờ cho ISBN này:
     *             nếu có → bản sao → Reserved; nếu không → Available</li>
     *         </ul>
     *       </li>
     *     </ul>
     *   </li>
     *   <li>Trả về ReturnResultDTO chứa đầy đủ thông tin kết quả</li>
     * </ol>
     *
     * @param barcode       mã vạch bản sao sách được trả
     * @param isDamaged     true nếu sách bị hư hỏng
     * @param isLost        true nếu sách bị mất (khai báo mất)
     * @param damagePercent phần trăm phạt hư hỏng (100-200), chỉ dùng khi isDamaged=true
     * @return ReturnResultDTO chứa kết quả xử lý trả sách
     * @throws BookNotAvailableException nếu không tìm thấy bản sao hoặc không có phiếu mượn active
     */
    public ReturnResultDTO processReturn(String barcode, boolean isDamaged, boolean isLost, double damagePercent) {
        // Kiểm tra dữ liệu đầu vào
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã vạch (barcode) không được để trống.");
        }

        Connection conn = null;
        try {
            // Lấy connection và bắt đầu transaction
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // ===== BƯỚC 1: Tìm bản sao sách theo barcode =====
            Optional<BookCopy> copyOpt = bookCopyRepository.findByBarcode(barcode);
            if (copyOpt.isEmpty()) {
                throw new BookNotAvailableException("Không tìm thấy bản sao sách với mã vạch '" + barcode + "'.");
            }
            BookCopy copy = copyOpt.get();

            // ===== BƯỚC 2: Tìm phiếu mượn đang hoạt động cho bản sao này =====
            Optional<Loan> activeLoanOpt = loanRepository.findActiveLoanByCopyId(copy.getCopyId());
            if (activeLoanOpt.isEmpty()) {
                throw new BookNotAvailableException(
                        "Không tìm thấy phiếu mượn đang hoạt động cho bản sao '" + barcode + "'.");
            }
            Loan loan = activeLoanOpt.get();

            // ===== BƯỚC 3: Ghi nhận ngày trả =====
            LocalDateTime returnDate = LocalDateTime.now();

            // Khởi tạo biến kết quả
            long overdueDays = 0;
            double fineAmount = 0;
            CopyStatus copyNewStatus;
            LoanStatus newLoanStatus;
            String message;

            // ===== BƯỚC 4-6: Xử lý theo từng trường hợp =====
            if (isDamaged) {
                // ---- TRƯỜNG HỢP: SÁCH HƯ HỎNG ----
                newLoanStatus = LoanStatus.DAMAGED_RETURN;
                copyNewStatus = CopyStatus.Damaged;

                // Tính phạt hư hỏng: damagePercent% giá bản sao (mặc định 100%)
                double percent = (damagePercent >= 100 && damagePercent <= 200) ? damagePercent : 100;
                fineAmount = copy.getPrice() * (percent / 100.0);

                // Tạo hóa đơn phạt
                String reason = String.format("Phạt hư hỏng sách (%.0f%% giá trị bản sao)", percent);
                fineService.createFine(loan.getLoanId(), fineAmount, reason);

                // Cập nhật trạng thái phiếu mượn
                loanRepository.updateReturnStatus(loan.getLoanId(), returnDate, newLoanStatus);

                // Cập nhật trạng thái bản sao
                bookCopyRepository.updateStatus(copy.getCopyId(), copyNewStatus);

                message = String.format("Sách bị hư hỏng. Phạt %.0f%% giá trị: %,.0f VNĐ", percent, fineAmount);

            } else if (isLost) {
                // ---- TRƯỜNG HỢP: SÁCH MẤT ----
                newLoanStatus = LoanStatus.LOST_RETURN;
                copyNewStatus = CopyStatus.Lost;

                // Tính phạt mất sách: 150% giá bản sao
                fineAmount = copy.getPrice() * 1.5;

                // Tạo hóa đơn phạt
                String reason = "Phạt mất sách (150% giá trị bản sao)";
                fineService.createFine(loan.getLoanId(), fineAmount, reason);

                // Cập nhật trạng thái phiếu mượn
                loanRepository.updateReturnStatus(loan.getLoanId(), returnDate, newLoanStatus);

                // Cập nhật trạng thái bản sao
                bookCopyRepository.updateStatus(copy.getCopyId(), copyNewStatus);

                message = String.format("Sách bị mất. Phạt 150%% giá trị: %,.0f VNĐ", fineAmount);

            } else {
                // ---- TRƯỜNG HỢP: TRẢ BÌNH THƯỜNG ----
                overdueDays = loan.calculateOverdueDays();

                if (overdueDays > 0) {
                    // Quá hạn — tạo phạt quá hạn
                    fineAmount = Fine.CALCULATE_OVERDUE_FINE(overdueDays); // 5.000 * overdueDays
                    String reason = String.format("Phạt quá hạn %d ngày (5.000 VNĐ/ngày)", overdueDays);
                    fineService.createFine(loan.getLoanId(), fineAmount, reason);

                    newLoanStatus = LoanStatus.OVERDUE;
                    message = String.format("Trả sách quá hạn %d ngày. Phạt: %,.0f VNĐ", overdueDays, fineAmount);
                } else {
                    newLoanStatus = LoanStatus.RETURNED;
                    message = "Trả sách thành công. Đúng hạn.";
                }

                // Cập nhật trạng thái phiếu mượn
                loanRepository.updateReturnStatus(loan.getLoanId(), returnDate, newLoanStatus);

                // Kiểm tra đơn đặt trước đang chờ cho ISBN này
                List<Reservation> pendingReservations = reservationRepository.findPendingByIsbn(copy.getIsbn());

                if (!pendingReservations.isEmpty()) {
                    // Có đơn đặt trước đang chờ → chuyển bản sao sang Reserved
                    copyNewStatus = CopyStatus.Reserved;
                    bookCopyRepository.updateStatus(copy.getCopyId(), copyNewStatus);

                    // Cập nhật ngày hết hạn đặt trước (3 ngày kể từ hôm nay)
                    Reservation nextReservation = pendingReservations.get(0);
                    // Cập nhật reservation để thông báo cho độc giả đặt trước
                    message += String.format(" Bản sao được giữ cho đơn đặt trước: %s",
                            nextReservation.getReservationId());
                } else {
                    // Không có đơn đặt trước → bản sao Available
                    copyNewStatus = CopyStatus.Available;
                    bookCopyRepository.updateStatus(copy.getCopyId(), copyNewStatus);
                }
            }

            // Commit transaction
            conn.commit();

            // ===== Tạo và trả về ReturnResultDTO =====
            return new ReturnResultDTO(
                    loan.getLoanId(),
                    copy.getCopyId(),
                    barcode,
                    returnDate,
                    overdueDays,
                    fineAmount,
                    copyNewStatus.name(),
                    message
            );

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Lỗi hệ thống khi xử lý trả sách: " + e.getMessage(), e);
        } catch (BookNotAvailableException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Lỗi không xác định khi xử lý trả sách: " + e.getMessage(), e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    // ========================== CÁC PHƯƠNG THỨC HỖ TRỢ ==========================

    /**
     * Lấy danh sách tất cả phiếu mượn trong hệ thống.
     *
     * @return danh sách tất cả phiếu mượn
     */
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    /**
     * Lấy danh sách các phiếu mượn quá hạn.
     *
     * @return danh sách phiếu mượn quá hạn
     */
    public List<Loan> getOverdueLoans() {
        return loanRepository.findOverdueLoans();
    }

    /**
     * Tìm phiếu mượn theo mã.
     *
     * @param loanId mã phiếu mượn
     * @return Optional chứa phiếu mượn nếu tìm thấy
     */
    public Optional<Loan> getLoanById(String loanId) {
        if (loanId == null || loanId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã phiếu mượn không được để trống.");
        }
        return loanRepository.findById(loanId);
    }

    // ========================== PHƯƠNG THỨC QUẢN LÝ TRANSACTION ==========================

    /**
     * Rollback transaction một cách an toàn, bỏ qua ngoại lệ nếu có.
     *
     * @param conn kết nối cơ sở dữ liệu
     */
    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Lỗi khi rollback transaction: " + ex.getMessage());
            }
        }
    }

    /**
     * Khôi phục chế độ auto-commit và đóng connection nếu cần.
     *
     * @param conn kết nối cơ sở dữ liệu
     */
    private void restoreAutoCommit(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Lỗi khi khôi phục auto-commit: " + ex.getMessage());
            }
        }
    }
}
