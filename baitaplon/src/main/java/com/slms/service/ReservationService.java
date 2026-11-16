package com.slms.service;

import com.slms.model.Reader;
import com.slms.model.Reservation;
import com.slms.model.BookCopy;
import com.slms.model.enums.CopyStatus;
import com.slms.model.enums.ReservationStatus;
import com.slms.exception.ReaderNotEligibleException;
import com.slms.exception.BookNotAvailableException;
import com.slms.repository.ReservationRepository;
import com.slms.repository.BookCopyRepository;
import com.slms.repository.ReaderRepository;
import com.slms.repository.BookRepository;
import com.slms.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lớp dịch vụ quản lý đặt trước sách trong hệ thống thư viện.
 * Xử lý việc tạo, hủy và quản lý các đơn đặt trước sách.
 *
 * <p>Quy tắc đặt trước:</p>
 * <ul>
 *   <li>Độc giả phải có trạng thái ACTIVE, thẻ còn hạn, nợ phạt ≤ 50.000 VNĐ</li>
 *   <li>Sách phải không còn bản sao nào có sẵn (tất cả đều đang mượn)</li>
 *   <li>Hạn nhận sách: 3 ngày kể từ khi bản sao được phân cho người đặt đầu tiên</li>
 *   <li>Trạng thái ban đầu: Pending</li>
 * </ul>
 *
 * @author SLMS Team
 */
public class ReservationService {

    private static final Object EXPIRY_PROCESS_LOCK = new Object();

    private final ReservationRepository reservationRepository;
    private final BookCopyRepository bookCopyRepository;
    private final ReaderRepository readerRepository;
    private final BookRepository bookRepository;

    /**
     * Khởi tạo ReservationService với các repository mặc định.
     */
    public ReservationService() {
        this.reservationRepository = new ReservationRepository();
        this.bookCopyRepository = new BookCopyRepository();
        this.readerRepository = new ReaderRepository();
        this.bookRepository = new BookRepository();
    }

    // ========================== ĐẶT TRƯỚC SÁCH ==========================

    /**
     * Tạo đơn đặt trước sách mới.
     *
     * <p>Quy trình xử lý:</p>
     * <ol>
     *   <li>Kiểm tra độc giả: tồn tại, trạng thái ACTIVE, thẻ còn hạn, nợ phạt ≤ 50.000 VNĐ</li>
     *   <li>Kiểm tra sách: phải không còn bản sao nào có sẵn (countAvailable = 0)</li>
     *   <li>Tạo đơn đặt trước với trạng thái Pending</li>
     *   <li>Ngày hết hạn được đặt 3 ngày; hệ thống bắt đầu tính lại khi bản sao được phân bổ</li>
     *   <li>Lưu vào cơ sở dữ liệu</li>
     * </ol>
     *
     * @param readerId mã độc giả muốn đặt trước
     * @param isbn     mã ISBN của sách cần đặt
     * @return đối tượng Reservation đã được tạo
     * @throws ReaderNotEligibleException nếu độc giả không đủ điều kiện đặt trước
     * @throws BookNotAvailableException  nếu sách vẫn còn bản sao có sẵn (không cần đặt trước)
     */
    public Reservation createReservation(String readerId, String isbn) {
        // Kiểm tra dữ liệu đầu vào
        if (readerId == null || readerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã độc giả không được để trống.");
        }
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã ISBN không được để trống.");
        }

        // 1. Kiểm tra độc giả
        Optional<Reader> readerOpt = readerRepository.findById(readerId);
        if (readerOpt.isEmpty()) {
            throw new ReaderNotEligibleException("Không tìm thấy độc giả với mã '" + readerId + "'.");
        }

        Reader reader = readerOpt.get();

        // Kiểm tra trạng thái độc giả (sử dụng canBorrow với 0 sách đang mượn để kiểm tra tư cách)
        // canBorrow kiểm tra: trạng thái ACTIVE, thẻ còn hạn, nợ phạt ≤ 50.000 VNĐ
        if (!reader.canBorrow(0)) {
            throw new ReaderNotEligibleException(
                    "Độc giả không đủ điều kiện đặt trước sách. " +
                    "Kiểm tra trạng thái thẻ, hạn sử dụng và số dư nợ phạt.");
        }

        if (bookRepository.findByIsbn(isbn).isEmpty()) {
            throw new BookNotAvailableException("Không tìm thấy đầu sách với ISBN '" + isbn + "'.");
        }

        // 2. Kiểm tra sách phải không còn bản sao nào có sẵn
        int availableCount = bookCopyRepository.countAvailableByIsbn(isbn);
        if (availableCount > 0) {
            throw new BookNotAvailableException(
                    "Sách vẫn còn " + availableCount + " bản sao có sẵn. " +
                    "Vui lòng mượn trực tiếp thay vì đặt trước.");
        }

        // 3. Tạo đơn đặt trước
        String reservationId = IdGenerator.generateReservationId();
        LocalDateTime reservationDate = LocalDateTime.now();
        // The date is refreshed when this reservation receives a returned copy.
        LocalDateTime expiryDate = reservationDate.plusDays(3);

        Reservation reservation = new Reservation(
                reservationId,
                readerId,
                isbn,
                reservationDate,
                expiryDate,
                ReservationStatus.Pending
        );

        // 4. Lưu vào cơ sở dữ liệu
        reservationRepository.save(reservation);

        return reservation;
    }

    // ========================== XỬ LÝ ĐƠN HẾT HẠN ==========================

    /**
     * Xử lý tất cả các đơn đặt trước đã hết hạn.
     *
     * <p>Quy trình xử lý:</p>
     * Chỉ các đơn đã được phân bản sao mới bắt đầu thời hạn 3 ngày. Khi hết hạn,
     * bản sao được chuyển cho người tiếp theo trong hàng chờ hoặc trả về Available.
     *
     * @return số lượng đơn đặt trước đã được xử lý (đánh dấu hết hạn)
     */
    public int processExpiredReservations() {
        synchronized (EXPIRY_PROCESS_LOCK) {
            return processExpiredReservationsLocked();
        }
    }

    private int processExpiredReservationsLocked() {
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations();
        Set<String> isbns = new HashSet<>();
        for (Reservation reservation : expiredReservations) {
            isbns.add(reservation.getIsbn());
        }
        int processedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (String isbn : isbns) {
            while (true) {
                List<Reservation> pending = reservationRepository.findPendingByIsbn(isbn);
                List<BookCopy> reservedCopies = getReservedCopies(isbn);
                int assignedCount = Math.min(pending.size(), reservedCopies.size());
                int expiredIndex = -1;

                for (int i = 0; i < assignedCount; i++) {
                    LocalDateTime expiryDate = pending.get(i).getExpiryDate();
                    if (expiryDate != null && !expiryDate.isAfter(now)) {
                        expiredIndex = i;
                        break;
                    }
                }

                if (expiredIndex < 0) {
                    break;
                }

                Reservation expired = pending.get(expiredIndex);
                if (!reservationRepository.updateStatus(expired.getReservationId(), ReservationStatus.Expired)) {
                    break;
                }
                processedCount++;

                List<Reservation> remaining = reservationRepository.findPendingByIsbn(isbn);
                if (remaining.size() < reservedCopies.size()) {
                    bookCopyRepository.updateStatus(
                            reservedCopies.get(reservedCopies.size() - 1).getCopyId(),
                            CopyStatus.Available
                    );
                } else {
                    Reservation newlyAssigned = remaining.get(reservedCopies.size() - 1);
                    reservationRepository.updateExpiryDate(
                            newlyAssigned.getReservationId(),
                            now.plusDays(3)
                    );
                }
            }
        }

        return processedCount;
    }

    // ========================== TRUY VẤN ĐƠN ĐẶT TRƯỚC ==========================

    /**
     * Lấy danh sách tất cả đơn đặt trước trong hệ thống.
     *
     * @return danh sách tất cả đơn đặt trước
     */
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    /**
     * Lấy danh sách đơn đặt trước của một độc giả.
     *
     * @param readerId mã độc giả
     * @return danh sách đơn đặt trước
     */
    public List<Reservation> getReservationsByReaderId(String readerId) {
        if (readerId == null || readerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã độc giả không được để trống.");
        }
        return reservationRepository.findByReaderId(readerId.trim());
    }

    /**
     * Lấy danh sách các đơn đặt trước đang chờ (Pending) cho một cuốn sách.
     *
     * @param isbn mã ISBN của sách
     * @return danh sách đơn đặt trước đang chờ
     * @throws IllegalArgumentException nếu ISBN để trống
     */
    public List<Reservation> getPendingByIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã ISBN không được để trống.");
        }
        return reservationRepository.findPendingByIsbn(isbn);
    }

    /**
     * Hủy đơn đặt trước sách.
     *
     * <p>Quy trình xử lý:</p>
     * <ol>
     *   <li>Tìm đơn đặt trước theo mã</li>
     *   <li>Kiểm tra trạng thái - chỉ hủy đơn đang Pending</li>
     *   <li>Cập nhật trạng thái sang Cancelled</li>
     *   <li>Nếu có bản sao đang Reserved cho đơn này, chuyển về Available</li>
     * </ol>
     *
     * @param reservationId mã đơn đặt trước cần hủy
     * @return true nếu hủy thành công, false nếu không tìm thấy đơn
     * @throws IllegalArgumentException nếu mã đơn đặt trước để trống
     * @throws IllegalStateException    nếu đơn không ở trạng thái Pending
     */
    public boolean cancelReservation(String reservationId) {
        if (reservationId == null || reservationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã đơn đặt trước không được để trống.");
        }

        processExpiredReservations();

        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            return false;
        }

        Reservation reservation = reservationOpt.get();

        // Chỉ cho phép hủy đơn đang Pending
        if (reservation.getStatus() != ReservationStatus.Pending) {
            throw new IllegalStateException(
                    "Chỉ có thể hủy đơn đặt trước đang ở trạng thái Pending. " +
                    "Trạng thái hiện tại: " + reservation.getStatus());
        }

        List<Reservation> pendingBefore = reservationRepository.findPendingByIsbn(reservation.getIsbn());
        int reservationIndex = -1;
        for (int i = 0; i < pendingBefore.size(); i++) {
            if (pendingBefore.get(i).getReservationId().equals(reservationId)) {
                reservationIndex = i;
                break;
            }
        }
        List<BookCopy> reservedCopies = getReservedCopies(reservation.getIsbn());

        boolean updated = reservationRepository.updateStatus(reservationId, ReservationStatus.Cancelled);
        if (updated && reservationIndex >= 0 && reservationIndex < reservedCopies.size()) {
            List<Reservation> remaining = reservationRepository.findPendingByIsbn(reservation.getIsbn());
            if (remaining.size() < reservedCopies.size()) {
                bookCopyRepository.updateStatus(
                        reservedCopies.get(reservedCopies.size() - 1).getCopyId(),
                        CopyStatus.Available
                );
            } else {
                Reservation newlyAssigned = remaining.get(reservedCopies.size() - 1);
                reservationRepository.updateExpiryDate(
                        newlyAssigned.getReservationId(),
                        LocalDateTime.now().plusDays(3)
                );
            }
        }

        return updated;
    }

    private List<BookCopy> getReservedCopies(String isbn) {
        return bookCopyRepository.findByIsbn(isbn).stream()
                .filter(copy -> copy.getStatus() == CopyStatus.Reserved)
                .toList();
    }
}
