package com.slms.service;

import com.slms.model.Book;
import com.slms.model.Fine;
import com.slms.model.Loan;
import com.slms.model.Reservation;
import com.slms.model.enums.LoanStatus;
import com.slms.model.enums.ReservationStatus;
import com.slms.repository.BookRepository;
import com.slms.repository.FineRepository;
import com.slms.repository.LoanRepository;
import com.slms.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Nghiệp vụ cổng độc giả: thông báo, thống kê, vị trí hàng chờ.
 */
public class ReaderPortalService {

    private final LoanRepository loanRepository = new LoanRepository();
    private final FineRepository fineRepository = new FineRepository();
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final BookRepository bookRepository = new BookRepository();

    public Map<String, Object> getPersonalStats(String readerId) {
        List<Loan> loans = loanRepository.findByReaderId(readerId);
        List<Fine> unpaid = fineRepository.findUnpaidFinesByReaderId(readerId);
        List<Reservation> reservations = reservationRepository.findByReaderId(readerId);

        long active = loans.stream().filter(l -> l.getStatus() == LoanStatus.BORROWED).count();
        long overdue = loans.stream().filter(l ->
                l.getStatus() == LoanStatus.OVERDUE
                        || (l.getStatus() == LoanStatus.BORROWED
                        && l.getDueDate() != null
                        && l.getDueDate().isBefore(LocalDateTime.now()))
        ).count();
        long returned = loans.stream().filter(l -> l.getStatus() == LoanStatus.RETURNED).count();
        long pendingRes = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.Pending).count();
        double unpaidAmount = unpaid.stream().mapToDouble(Fine::getFineAmount).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLoans", loans.size());
        stats.put("activeLoans", active);
        stats.put("overdueLoans", overdue);
        stats.put("returnedLoans", returned);
        stats.put("pendingReservations", pendingRes);
        stats.put("unpaidFineCount", unpaid.size());
        stats.put("unpaidFineAmount", unpaidAmount);
        return stats;
    }

    public List<Map<String, Object>> getNotifications(String readerId) {
        List<Map<String, Object>> notes = new ArrayList<>();

        for (Loan loan : loanRepository.findByReaderId(readerId)) {
            if (loan.getStatus() == LoanStatus.BORROWED && loan.getDueDate() != null) {
                long days = java.time.Duration.between(LocalDateTime.now(), loan.getDueDate()).toDays();
                if (loan.getDueDate().isBefore(LocalDateTime.now())) {
                    Map<String, Object> n = new HashMap<>();
                    n.put("type", "OVERDUE");
                    n.put("title", "Sách quá hạn");
                    n.put("message", "Phiếu " + loan.getLoanId() + " đã quá hạn. Vui lòng trả tại quầy.");
                    n.put("refId", loan.getLoanId());
                    notes.add(n);
                } else if (days <= 3) {
                    Map<String, Object> n = new HashMap<>();
                    n.put("type", "DUE_SOON");
                    n.put("title", "Sắp đến hạn trả");
                    n.put("message", "Phiếu " + loan.getLoanId() + " còn khoảng " + Math.max(days, 0) + " ngày.");
                    n.put("refId", loan.getLoanId());
                    notes.add(n);
                }
            }
        }

        for (Reservation r : reservationRepository.findByReaderId(readerId)) {
            if (r.getStatus() == ReservationStatus.Pending) {
                int position = getQueuePosition(r.getIsbn(), r.getReservationId());
                Map<String, Object> n = new HashMap<>();
                n.put("type", "RESERVATION");
                n.put("title", "Đơn đặt trước đang chờ");
                n.put("message", "ISBN " + r.getIsbn() + " — vị trí hàng chờ #" + position);
                n.put("refId", r.getReservationId());
                n.put("queuePosition", position);
                notes.add(n);
            } else if (r.getStatus() == ReservationStatus.Fulfilled
                    || "Ready".equalsIgnoreCase(String.valueOf(r.getStatus()))) {
                Map<String, Object> n = new HashMap<>();
                n.put("type", "READY");
                n.put("title", "Sách đặt trước đã sẵn sàng");
                n.put("message", "Đơn " + r.getReservationId() + " — đến quầy nhận trong hạn giữ chỗ.");
                n.put("refId", r.getReservationId());
                notes.add(n);
            }
        }

        List<Fine> unpaid = fineRepository.findUnpaidFinesByReaderId(readerId);
        if (!unpaid.isEmpty()) {
            double total = unpaid.stream().mapToDouble(Fine::getFineAmount).sum();
            Map<String, Object> n = new HashMap<>();
            n.put("type", "FINE");
            n.put("title", "Có khoản phạt chưa thanh toán");
            n.put("message", "Tổng nợ: " + String.format("%,.0f", total) + " VNĐ (" + unpaid.size() + " hóa đơn).");
            n.put("refId", null);
            notes.add(n);
        }

        return notes;
    }

    public Map<String, Object> getReservationQueueInfo(String reservationId) {
        Optional<Reservation> opt = reservationRepository.findById(reservationId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy đơn đặt trước");
        }
        Reservation r = opt.get();
        int position = getQueuePosition(r.getIsbn(), r.getReservationId());
        List<Reservation> pending = reservationRepository.findPendingByIsbn(r.getIsbn());

        Optional<Book> book = bookRepository.findByIsbn(r.getIsbn());
        Map<String, Object> info = new HashMap<>();
        info.put("reservationId", r.getReservationId());
        info.put("isbn", r.getIsbn());
        info.put("bookTitle", book.map(Book::getTitle).orElse(r.getIsbn()));
        info.put("status", r.getStatus() != null ? r.getStatus().name() : null);
        info.put("queuePosition", position);
        info.put("queueLength", pending.size());
        info.put("expiryDate", r.getExpiryDate() != null ? r.getExpiryDate().toString() : null);
        return info;
    }

    private int getQueuePosition(String isbn, String reservationId) {
        List<Reservation> pending = reservationRepository.findPendingByIsbn(isbn);
        int pos = 1;
        for (Reservation r : pending) {
            if (r.getReservationId().equals(reservationId)) {
                return pos;
            }
            pos++;
        }
        return pending.isEmpty() ? 0 : pending.size();
    }
}
