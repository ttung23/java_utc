package com.slms.repository;

import com.slms.model.Fine;
import com.slms.model.enums.PaymentStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thao tác CRUD đối với bảng FINES trong cơ sở dữ liệu.
 * Quản lý các khoản phạt liên quan đến việc mượn trả sách.
 *
 * @author SLMS Team
 * @version 1.0
 */
public class FineRepository {

    // ==================== Câu lệnh SQL ====================
    private static final String SQL_INSERT =
            "INSERT INTO FINES (fine_id, loan_id, reason, fine_amount, payment_status, payment_date) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_ALL =
            "SELECT fine_id, loan_id, reason, fine_amount, payment_status, payment_date "
                    + "FROM FINES ORDER BY fine_id";

    private static final String SQL_FIND_BY_ID =
            "SELECT fine_id, loan_id, reason, fine_amount, payment_status, payment_date "
                    + "FROM FINES WHERE fine_id = ?";

    private static final String SQL_FIND_BY_LOAN_ID =
            "SELECT fine_id, loan_id, reason, fine_amount, payment_status, payment_date "
                    + "FROM FINES WHERE loan_id = ?";

    private static final String SQL_FIND_UNPAID_BY_READER =
            "SELECT f.fine_id, f.loan_id, f.reason, f.fine_amount, f.payment_status, f.payment_date "
                    + "FROM FINES f JOIN LOANS l ON f.loan_id = l.loan_id "
                    + "WHERE l.reader_id = ? AND f.payment_status = 'Unpaid'";

    private static final String SQL_UPDATE_PAYMENT =
            "UPDATE FINES SET payment_status = ?, payment_date = ? WHERE fine_id = ?";

    /**
     * Lưu khoản phạt mới vào cơ sở dữ liệu.
     *
     * @param fine đối tượng khoản phạt cần lưu
     * @return đối tượng khoản phạt đã được lưu
     */
    public Fine save(Fine fine) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, fine.getFineId());
            ps.setString(2, fine.getLoanId());
            ps.setString(3, fine.getReason());
            ps.setDouble(4, fine.getFineAmount());
            ps.setString(5, fine.getPaymentStatus().name());

            // payment_date có thể null (chưa thanh toán)
            if (fine.getPaymentDate() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(fine.getPaymentDate()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lưu khoản phạt [" + fine.getFineId() + "]: " + e.getMessage(), e);
        }
        return fine;
    }

    /**
     * Lấy danh sách tất cả khoản phạt.
     *
     * @return danh sách tất cả khoản phạt
     */
    public List<Fine> findAll() {
        List<Fine> fines = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                fines.add(mapResultSetToFine(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách khoản phạt: " + e.getMessage(), e);
        }
        return fines;
    }

    /**
     * Tìm khoản phạt theo mã định danh.
     *
     * @param fineId mã khoản phạt cần tìm
     * @return {@link Optional} chứa khoản phạt nếu tìm thấy
     */
    public Optional<Fine> findById(String fineId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setString(1, fineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToFine(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm khoản phạt theo ID [" + fineId + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Tìm tất cả khoản phạt theo mã phiếu mượn.
     *
     * @param loanId mã phiếu mượn
     * @return danh sách khoản phạt liên quan đến phiếu mượn
     */
    public List<Fine> findByLoanId(String loanId) {
        List<Fine> fines = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_LOAN_ID)) {

            ps.setString(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fines.add(mapResultSetToFine(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm khoản phạt theo phiếu mượn [" + loanId + "]: " + e.getMessage(), e);
        }
        return fines;
    }

    /**
     * Tìm tất cả khoản phạt chưa thanh toán của một bạn đọc.
     * Sử dụng JOIN với bảng LOANS để tìm theo reader_id.
     *
     * @param readerId mã bạn đọc
     * @return danh sách khoản phạt chưa thanh toán
     */
    public List<Fine> findUnpaidFinesByReaderId(String readerId) {
        List<Fine> fines = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_UNPAID_BY_READER)) {

            ps.setString(1, readerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fines.add(mapResultSetToFine(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm khoản phạt chưa thanh toán của bạn đọc [" + readerId + "]: "
                    + e.getMessage(), e);
        }
        return fines;
    }

    /**
     * Cập nhật trạng thái thanh toán khoản phạt.
     *
     * @param fineId      mã khoản phạt
     * @param status      trạng thái thanh toán mới
     * @param paymentDate ngày giờ thanh toán
     * @return {@code true} nếu cập nhật thành công
     */
    public boolean updatePaymentStatus(String fineId, PaymentStatus status, LocalDateTime paymentDate) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PAYMENT)) {

            ps.setString(1, status.name());

            // Chuyển đổi LocalDateTime sang Timestamp
            if (paymentDate != null) {
                ps.setTimestamp(2, Timestamp.valueOf(paymentDate));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }

            ps.setString(3, fineId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật trạng thái thanh toán [" + fineId + "]: " + e.getMessage(), e);
        }
    }

    // ==================== Phương thức hỗ trợ ====================

    /**
     * Chuyển đổi một dòng ResultSet thành đối tượng Fine.
     *
     * @param rs ResultSet đang trỏ đến dòng cần chuyển đổi
     * @return đối tượng Fine được ánh xạ từ ResultSet
     * @throws SQLException nếu có lỗi khi đọc dữ liệu
     */
    private Fine mapResultSetToFine(ResultSet rs) throws SQLException {
        Fine fine = new Fine();
        fine.setFineId(rs.getString("fine_id"));
        fine.setLoanId(rs.getString("loan_id"));
        fine.setReason(rs.getString("reason"));
        fine.setFineAmount(rs.getDouble("fine_amount"));

        // Chuyển đổi chuỗi sang enum PaymentStatus
        fine.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));

        // payment_date có thể null (chưa thanh toán)
        Timestamp paymentTs = rs.getTimestamp("payment_date");
        if (paymentTs != null) {
            fine.setPaymentDate(paymentTs.toLocalDateTime());
        }

        return fine;
    }
}
