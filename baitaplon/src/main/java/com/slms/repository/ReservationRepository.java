package com.slms.repository;

import com.slms.model.Reservation;
import com.slms.model.enums.ReservationStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thao tác CRUD đối với bảng RESERVATIONS trong cơ sở dữ liệu.
 * Quản lý việc đặt trước sách của bạn đọc.
 *
 * @author SLMS Team
 * @version 1.0
 */
public class ReservationRepository {

    // ==================== Câu lệnh SQL ====================
    private static final String SQL_INSERT =
            "INSERT INTO RESERVATIONS (reservation_id, reader_id, isbn, reservation_date, expiry_date, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_ALL =
            "SELECT reservation_id, reader_id, isbn, reservation_date, expiry_date, status "
                    + "FROM RESERVATIONS ORDER BY reservation_date DESC";

    private static final String SQL_FIND_BY_ID =
            "SELECT reservation_id, reader_id, isbn, reservation_date, expiry_date, status "
                    + "FROM RESERVATIONS WHERE reservation_id = ?";

    private static final String SQL_FIND_PENDING_BY_ISBN =
            "SELECT reservation_id, reader_id, isbn, reservation_date, expiry_date, status "
                    + "FROM RESERVATIONS WHERE isbn = ? AND status = 'Pending' ORDER BY reservation_date ASC";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE RESERVATIONS SET status = ? WHERE reservation_id = ?";

    private static final String SQL_FIND_EXPIRED =
            "SELECT reservation_id, reader_id, isbn, reservation_date, expiry_date, status "
                    + "FROM RESERVATIONS WHERE status = 'Pending' AND expiry_date < CURRENT_TIMESTAMP";

    /**
     * Lưu lượt đặt trước mới vào cơ sở dữ liệu.
     *
     * @param reservation đối tượng đặt trước cần lưu
     * @return đối tượng đặt trước đã được lưu
     */
    public Reservation save(Reservation reservation) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, reservation.getReservationId());
            ps.setString(2, reservation.getReaderId());
            ps.setString(3, reservation.getIsbn());
            // Chuyển đổi LocalDateTime sang Timestamp
            ps.setTimestamp(4, Timestamp.valueOf(reservation.getReservationDate()));
            ps.setTimestamp(5, Timestamp.valueOf(reservation.getExpiryDate()));
            ps.setString(6, reservation.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lưu đặt trước [" + reservation.getReservationId() + "]: "
                    + e.getMessage(), e);
        }
        return reservation;
    }

    /**
     * Lấy danh sách tất cả lượt đặt trước, sắp xếp theo ngày đặt giảm dần.
     *
     * @return danh sách tất cả lượt đặt trước
     */
    public List<Reservation> findAll() {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách đặt trước: " + e.getMessage(), e);
        }
        return reservations;
    }

    /**
     * Tìm lượt đặt trước theo mã định danh.
     *
     * @param reservationId mã đặt trước cần tìm
     * @return {@link Optional} chứa lượt đặt trước nếu tìm thấy
     */
    public Optional<Reservation> findById(String reservationId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setString(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm đặt trước theo ID [" + reservationId + "]: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Tìm các lượt đặt trước đang chờ (Pending) theo ISBN.
     * Sắp xếp theo ngày đặt tăng dần (người đặt trước được ưu tiên).
     *
     * @param isbn mã ISBN của sách
     * @return danh sách lượt đặt trước đang chờ
     */
    public List<Reservation> findPendingByIsbn(String isbn) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_PENDING_BY_ISBN)) {

            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSetToReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm đặt trước theo ISBN [" + isbn + "]: " + e.getMessage(), e);
        }
        return reservations;
    }

    /**
     * Cập nhật trạng thái lượt đặt trước.
     *
     * @param reservationId mã đặt trước
     * @param status        trạng thái mới
     * @return {@code true} nếu cập nhật thành công
     */
    public boolean updateStatus(String reservationId, ReservationStatus status) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {

            ps.setString(1, status.name());
            ps.setString(2, reservationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật trạng thái đặt trước [" + reservationId + "]: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Tìm các lượt đặt trước đã hết hạn.
     * Lượt đặt trước hết hạn là có trạng thái Pending và ngày hết hạn đã qua.
     *
     * @return danh sách lượt đặt trước đã hết hạn
     */
    public List<Reservation> findExpiredReservations() {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_EXPIRED);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm đặt trước hết hạn: " + e.getMessage(), e);
        }
        return reservations;
    }

    // ==================== Phương thức hỗ trợ ====================

    /**
     * Chuyển đổi một dòng ResultSet thành đối tượng Reservation.
     *
     * @param rs ResultSet đang trỏ đến dòng cần chuyển đổi
     * @return đối tượng Reservation được ánh xạ từ ResultSet
     * @throws SQLException nếu có lỗi khi đọc dữ liệu
     */
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setReservationId(rs.getString("reservation_id"));
        reservation.setReaderId(rs.getString("reader_id"));
        reservation.setIsbn(rs.getString("isbn"));

        // Chuyển đổi Timestamp sang LocalDateTime
        Timestamp reservationTs = rs.getTimestamp("reservation_date");
        if (reservationTs != null) {
            reservation.setReservationDate(reservationTs.toLocalDateTime());
        }

        Timestamp expiryTs = rs.getTimestamp("expiry_date");
        if (expiryTs != null) {
            reservation.setExpiryDate(expiryTs.toLocalDateTime());
        }

        // Chuyển đổi chuỗi sang enum ReservationStatus
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        return reservation;
    }
}
