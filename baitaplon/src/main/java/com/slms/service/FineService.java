package com.slms.service;

import com.slms.model.Fine;
import com.slms.model.Loan;
import com.slms.model.Reader;
import com.slms.model.enums.PaymentStatus;
import com.slms.repository.FineRepository;
import com.slms.repository.LoanRepository;
import com.slms.repository.ReaderRepository;
import com.slms.util.IdGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lớp dịch vụ quản lý tiền phạt trong hệ thống thư viện.
 * Xử lý tính toán, tạo, thanh toán và truy vấn các khoản phạt.
 *
 * <p>Các loại phạt trong hệ thống:</p>
 * <ul>
 *   <li>Phạt quá hạn: 5.000 VNĐ/ngày/sách</li>
 *   <li>Phạt hư hỏng: 100-200% giá trị bản sao sách</li>
 *   <li>Phạt mất sách: 150% giá trị bản sao sách</li>
 * </ul>
 *
 * @author SLMS Team
 */
public class FineService {

    private final FineRepository fineRepository;
    private final ReaderRepository readerRepository;
    private final LoanRepository loanRepository;

    /**
     * Khởi tạo FineService với các repository mặc định.
     */
    public FineService() {
        this.fineRepository = new FineRepository();
        this.readerRepository = new ReaderRepository();
        this.loanRepository = new LoanRepository();
    }

    // ========================== TÍNH TOÁN TIỀN PHẠT ==========================

    /**
     * Tính tiền phạt quá hạn dựa trên phiếu mượn.
     * Công thức: số ngày quá hạn × 5.000 VNĐ
     *
     * @param loan phiếu mượn cần tính phạt
     * @return số tiền phạt quá hạn (VNĐ), trả về 0 nếu chưa quá hạn
     */
    public double calculateOverdueFine(Loan loan) {
        if (loan == null) {
            return 0;
        }
        long overdueDays = loan.calculateOverdueDays();
        return Fine.CALCULATE_OVERDUE_FINE(overdueDays);
    }

    // ========================== TẠO HÓA ĐƠN PHẠT ==========================

    /**
     * Tạo hóa đơn phạt mới và cập nhật số dư nợ phạt của độc giả.
     *
     * <p>Quy trình xử lý:</p>
     * <ol>
     *   <li>Tạo mã phạt mới bằng IdGenerator</li>
     *   <li>Tạo đối tượng Fine với trạng thái Unpaid</li>
     *   <li>Lưu hóa đơn phạt vào cơ sở dữ liệu</li>
     *   <li>Cập nhật số dư nợ phạt của độc giả (cộng thêm số tiền phạt)</li>
     * </ol>
     *
     * @param loanId  mã phiếu mượn liên quan đến khoản phạt
     * @param amount  số tiền phạt (VNĐ)
     * @param reason  lý do phạt (ví dụ: "Quá hạn 5 ngày", "Hư hỏng sách")
     * @return đối tượng Fine đã được tạo và lưu
     * @throws IllegalArgumentException nếu số tiền phạt <= 0 hoặc phiếu mượn không tồn tại
     * @throws RuntimeException nếu không thể cập nhật số dư nợ phạt
     */
    public Fine createFine(String loanId, double amount, String reason) {
        // Kiểm tra dữ liệu đầu vào
        if (loanId == null || loanId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã phiếu mượn không được để trống.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền phạt phải lớn hơn 0.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do phạt không được để trống.");
        }

        // Kiểm tra phiếu mượn có tồn tại không
        Optional<Loan> loanOpt = loanRepository.findById(loanId);
        if (loanOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy phiếu mượn với mã '" + loanId + "'.");
        }

        Loan loan = loanOpt.get();

        // Tạo mã phạt mới
        String fineId = IdGenerator.generateFineId();

        // Tạo đối tượng Fine
        Fine fine = new Fine(fineId, loanId, reason, amount,
                PaymentStatus.Unpaid, null);

        // Lưu hóa đơn phạt vào DB
        fineRepository.save(fine);

        // Cập nhật số dư nợ phạt của độc giả (cộng thêm tiền phạt)
        String readerId = loan.getReaderId();
        Optional<Reader> readerOpt = readerRepository.findById(readerId);
        if (readerOpt.isPresent()) {
            Reader reader = readerOpt.get();
            double newBalance = reader.getBalanceFine() + amount;
            readerRepository.updateBalanceFine(readerId, newBalance);
        }

        return fine;
    }

    // ========================== THANH TOÁN TIỀN PHẠT ==========================

    /**
     * Thanh toán một khoản phạt.
     *
     * <p>Quy trình xử lý:</p>
     * <ol>
     *   <li>Tìm hóa đơn phạt theo mã</li>
     *   <li>Kiểm tra trạng thái - chỉ thanh toán khoản chưa thanh toán</li>
     *   <li>Cập nhật trạng thái thanh toán sang Paid, ghi nhận ngày thanh toán</li>
     *   <li>Giảm số dư nợ phạt của độc giả tương ứng</li>
     * </ol>
     *
     * @param fineId mã hóa đơn phạt cần thanh toán
     * @return true nếu thanh toán thành công, false nếu không tìm thấy
     * @throws IllegalArgumentException nếu mã phạt để trống
     * @throws IllegalStateException    nếu khoản phạt đã được thanh toán trước đó
     */
    public boolean payFine(String fineId) {
        if (fineId == null || fineId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã hóa đơn phạt không được để trống.");
        }

        // Tìm hóa đơn phạt
        Optional<Fine> fineOpt = fineRepository.findById(fineId);
        if (fineOpt.isEmpty()) {
            return false;
        }

        Fine fine = fineOpt.get();

        // Kiểm tra trạng thái - không cho thanh toán lại
        if (fine.getPaymentStatus() == PaymentStatus.Paid) {
            throw new IllegalStateException("Hóa đơn phạt '" + fineId + "' đã được thanh toán trước đó.");
        }

        // Cập nhật trạng thái thanh toán
        LocalDateTime paymentDate = LocalDateTime.now();
        boolean updated = fineRepository.updatePaymentStatus(fineId, PaymentStatus.Paid, paymentDate);

        if (updated) {
            // Giảm số dư nợ phạt của độc giả
            // Lấy readerId từ phiếu mượn liên kết với khoản phạt
            Optional<Loan> loanOpt = loanRepository.findById(fine.getLoanId());
            if (loanOpt.isPresent()) {
                String readerId = loanOpt.get().getReaderId();
                Optional<Reader> readerOpt = readerRepository.findById(readerId);
                if (readerOpt.isPresent()) {
                    Reader reader = readerOpt.get();
                    double newBalance = Math.max(0, reader.getBalanceFine() - fine.getFineAmount());
                    readerRepository.updateBalanceFine(readerId, newBalance);
                }
            }
        }

        return updated;
    }

    // ========================== TRUY VẤN HÓA ĐƠN PHẠT ==========================

    /**
     * Lấy danh sách tất cả hóa đơn phạt trong hệ thống.
     *
     * @return danh sách tất cả hóa đơn phạt
     */
    public List<Fine> getAllFines() {
        return fineRepository.findAll();
    }

    /**
     * Lấy danh sách các khoản phạt chưa thanh toán của một độc giả.
     *
     * @param readerId mã độc giả
     * @return danh sách các khoản phạt chưa thanh toán
     * @throws IllegalArgumentException nếu mã độc giả để trống
     */
    public List<Fine> getUnpaidFinesByReaderId(String readerId) {
        if (readerId == null || readerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã độc giả không được để trống.");
        }
        return fineRepository.findUnpaidFinesByReaderId(readerId);
    }

    /**
     * Tìm hóa đơn phạt theo mã.
     *
     * @param fineId mã hóa đơn phạt
     * @return Optional chứa hóa đơn phạt nếu tìm thấy
     */
    public Optional<Fine> getFineById(String fineId) {
        if (fineId == null || fineId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã hóa đơn phạt không được để trống.");
        }
        return fineRepository.findById(fineId);
    }

    /**
     * Tìm hóa đơn phạt theo mã phiếu mượn.
     *
     * @param loanId mã phiếu mượn
     * @return Optional chứa hóa đơn phạt nếu tìm thấy
     */
    public Optional<Fine> getFineByLoanId(String loanId) {
        if (loanId == null || loanId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã phiếu mượn không được để trống.");
        }
        List<Fine> list = fineRepository.findByLoanId(loanId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
