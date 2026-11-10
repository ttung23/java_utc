package com.slms.repository;

import com.slms.model.Loan;
import com.slms.model.enums.LoanStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thao tác CRUD đối với bảng LOANS trong cơ sở dữ liệu.
 * Quản lý các phiếu mượn sách của bạn đọc.
 *
 * @author SLMS Team
 * @version 1.0
 */
public class LoanRepository {

    // ==================== Câu lệnh SQL ====================
    private static final String SQL_INSERT =
            "INSERT INTO LOANS (loan_id, reader_id, copy_id, borrow_date, due_date, return_date, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_ALL =
            "SELECT loan_id, reader_id, copy_id, borrow_date, due_date, return_date, status "
                    + "FROM LOANS ORDER BY borrow_date DESC";

    private static final String SQL_FIND_BY_ID =
            "SELECT loan_id, reader_id, copy_id, borrow_date, due_date, return_date, status "
                    + "FROM LOANS WHERE loan_id = ?";

    private static final String SQL_FIND_ACTIVE_BY_COPY_ID =
            "SELECT loan_id, reader_id, copy_id, borrow_date, due_date, return_date, status "
                    + "FROM LOANS WHERE copy_id = ? AND status = 'BORROWED'";

    private static final String SQL_COUNT_ACTIVE_BY_READER =
            "SELECT COUNT(*) FROM LOANS WHERE reader_id = ? AND status = 'BORROWED'";

    private static final String SQL_UPDATE_RETURN =
            "UPDATE LOANS SET return_date = ?, status = ? WHERE loan_id = ?";

    private static final String SQL_FIND_OVERDUE =
            "SELECT loan_id, reader_id, copy_id, borrow_date, due_date, return_date, status "
                    + "FROM LOANS WHERE status = 'BORROWED' AND due_date < CURRENT_TIMESTAMP";

    /**
     * Lưu phiếu mượn mới vào cơ sở dữ liệu.
     *
     * @param loan đối tượng phiếu mượn cần lưu
     * @return đối tượng phiếu mượn đã được lưu
     */
    public Loan save(Loan loan) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, loan.getLoanId());
            ps.setString(2, loan.getReaderId());
            ps.setString(3, loan.getCopyId());
            // Chuyển đổi LocalDateTime sang Timestamp
            ps.setTimestamp(4, Timestamp.valueOf(loan.getBorrowDate()));
            ps.setTimestamp(5, Timestamp.valueOf(loan.getDueDate()));

            // return_date có thể null (chưa trả)
            if (loan.getReturnDate() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(loan.getReturnDate()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }

            ps.setString(7, loan.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lưu phiếu mượn [" + loan.getLoanId() + "]: " + e.getMessage(), e);
        }
        return loan;
    }

    /**
     * Lấy danh sách tất cả phiếu mượn, sắp xếp theo ngày mượn giảm dần.
     *
     * @return danh sách tất cả phiếu mượn
     */
    public List<Loan> findAll() {
        List<Loan> loans = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                loans.add(mapResultSetToLoan(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách phiếu mượn: " + e.getMessage(), e);
        }
        return loans;
    }

    /**
     * Tìm phiếu mượn theo mã định danh.
     *
     * @param loanId mã phiếu mượn cần tìm
     * @return {@link Optional} chứa phiếu mượn nếu tìm thấy
     */
    public Optional<Loan> findById(String loanId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setString(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLoan(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm phiếu mượn theo ID [" + loanId + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Tìm phiếu mượn đang hoạt động (BORROWED) theo mã bản sao.
     * Dùng để kiểm tra bản sao có đang được mượn hay không.
     *
     * @param copyId mã bản sao sách
     * @return {@link Optional} chứa phiếu mượn đang hoạt động nếu tìm thấy
     */
    public Optional<Loan> findActiveLoanByCopyId(String copyId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ACTIVE_BY_COPY_ID)) {

            ps.setString(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLoan(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm phiếu mượn đang hoạt động theo bản sao [" + copyId + "]: "
                    + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Đếm số phiếu mượn đang hoạt động (BORROWED) của một bạn đọc.
     * Dùng để kiểm tra giới hạn số sách được mượn.
     *
     * @param readerId mã bạn đọc
     * @return số lượng phiếu mượn đang hoạt động
     */
    public int countActiveLoansByReaderId(String readerId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT_ACTIVE_BY_READER)) {

            ps.setString(1, readerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi đếm phiếu mượn đang hoạt động của bạn đọc [" + readerId + "]: "
                    + e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Cập nhật trạng thái trả sách cho phiếu mượn.
     *
     * @param loanId     mã phiếu mượn
     * @param returnDate ngày giờ trả sách
     * @param status     trạng thái mới (RETURNED, OVERDUE,...)
     * @return {@code true} nếu cập nhật thành công
     */
    public boolean updateReturnStatus(String loanId, LocalDateTime returnDate, LoanStatus status) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_RETURN)) {

            // Chuyển đổi LocalDateTime sang Timestamp
            ps.setTimestamp(1, Timestamp.valueOf(returnDate));
            ps.setString(2, status.name());
            ps.setString(3, loanId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật trạng thái trả sách [" + loanId + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Tìm danh sách các phiếu mượn quá hạn.
     * Phiếu mượn quá hạn là phiếu có trạng thái BORROWED và ngày đến hạn đã qua.
     *
     * @return danh sách phiếu mượn quá hạn
     */
    public List<Loan> findOverdueLoans() {
        List<Loan> loans = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_OVERDUE);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                loans.add(mapResultSetToLoan(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm phiếu mượn quá hạn: " + e.getMessage(), e);
        }
        return loans;
    }

    // ==================== Phương thức hỗ trợ ====================

    /**
     * Chuyển đổi một dòng ResultSet thành đối tượng Loan.
     *
     * @param rs ResultSet đang trỏ đến dòng cần chuyển đổi
     * @return đối tượng Loan được ánh xạ từ ResultSet
     * @throws SQLException nếu có lỗi khi đọc dữ liệu
     */
    private Loan mapResultSetToLoan(ResultSet rs) throws SQLException {
        Loan loan = new Loan();
        loan.setLoanId(rs.getString("loan_id"));
        loan.setReaderId(rs.getString("reader_id"));
        loan.setCopyId(rs.getString("copy_id"));

        // Chuyển đổi Timestamp sang LocalDateTime
        Timestamp borrowTs = rs.getTimestamp("borrow_date");
        if (borrowTs != null) {
            loan.setBorrowDate(borrowTs.toLocalDateTime());
        }

        Timestamp dueTs = rs.getTimestamp("due_date");
        if (dueTs != null) {
            loan.setDueDate(dueTs.toLocalDateTime());
        }

        // return_date có thể null (chưa trả sách)
        Timestamp returnTs = rs.getTimestamp("return_date");
        if (returnTs != null) {
            loan.setReturnDate(returnTs.toLocalDateTime());
        }

        // Chuyển đổi chuỗi trạng thái sang enum LoanStatus
        loan.setStatus(LoanStatus.valueOf(rs.getString("status")));
        return loan;
    }
}
