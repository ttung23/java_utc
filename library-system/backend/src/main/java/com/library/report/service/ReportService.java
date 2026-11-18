package com.library.report.service;

import com.library.borrow.repository.BorrowSlipItemRepository;
import com.library.borrow.repository.BorrowSlipRepository;
import com.library.catalog.entity.Book;
import com.library.catalog.repository.BookCopyRepository;
import com.library.catalog.repository.BookRepository;
import com.library.common.enums.BorrowChannel;
import com.library.common.enums.BorrowSlipStatus;
import com.library.common.enums.CopyStatus;
import com.library.common.excel.ExcelHelper;
import com.library.common.exception.BusinessException;
import com.library.fine.repository.FineRepository;
import com.library.reader.repository.ReaderRepository;
import com.library.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final ReaderRepository readerRepository;
    private final BorrowSlipRepository borrowSlipRepository;
    private final BorrowSlipItemRepository borrowSlipItemRepository;
    private final FineRepository fineRepository;
    private final UserRepository userRepository;

    public Map<String, Object> dashboard() {
        LocalDate today = LocalDate.now(ZONE);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalBooks", bookRepository.count());
        m.put("totalCopies", bookCopyRepository.count());
        m.put("availableCopies", bookCopyRepository.findAll().stream()
                .filter(c -> c.getDeletedAt() == null && c.getStatus() == CopyStatus.AVAILABLE).count());
        m.put("borrowingItems", borrowSlipItemRepository.findAll().stream()
                .filter(i -> i.getDeletedAt() == null
                        && i.getStatus() == com.library.common.enums.BorrowItemStatus.BORROWING)
                .count());
        m.put("overdueItems", borrowSlipItemRepository.findOverdueItems(today).size());
        m.put("pendingOnline", borrowSlipRepository
                .findByStatusAndChannelAndDeletedAtIsNull(BorrowSlipStatus.PENDING, BorrowChannel.ONLINE).size());
        m.put("totalReaders", readerRepository.count());
        m.put("unpaidFines", fineRepository.sumAllUnpaid());
        m.put("totalUsers", userRepository.count());
        m.put("asOf", today);

        // Demo KPI theo ngày (để dashboard đẹp khi chưa có giao dịch trong ngày)
        Instant start = today.atStartOfDay(ZONE).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(ZONE).toInstant();
        long borrowedToday = borrowSlipItemRepository.findAll().stream()
                .filter(i -> i.getDeletedAt() == null && i.getBorrowedAt() != null
                        && !i.getBorrowedAt().isBefore(start) && i.getBorrowedAt().isBefore(end))
                .count();
        long returnedToday = borrowSlipItemRepository.findAll().stream()
                .filter(i -> i.getDeletedAt() == null && i.getReturnedAt() != null
                        && !i.getReturnedAt().isBefore(start) && i.getReturnedAt().isBefore(end))
                .count();
        boolean demoDaily = borrowedToday == 0 && returnedToday == 0;
        if (demoDaily) {
            borrowedToday = 24;
            returnedToday = 18;
        }
        m.put("borrowedToday", borrowedToday);
        m.put("returnedToday", returnedToday);
        m.put("borrowedTodayCounter", demoDaily ? 18 : null);
        m.put("borrowedTodayOnline", demoDaily ? 6 : null);
        m.put("returnOnTimeRate", demoDaily ? 94.8 : null);
        m.put("demoDailyStats", demoDaily);

        long overdueCount = ((Number) m.get("overdueItems")).longValue();
        long pendingCount = ((Number) m.get("pendingOnline")).longValue();
        BigDecimal unpaid = (BigDecimal) m.get("unpaidFines");
        List<Map<String, Object>> urgent = new ArrayList<>();
        if (overdueCount > 0) {
            urgent.add(urgentTask("OVERDUE", "HIGH",
                    overdueCount + " sách quá hạn cần thu hồi",
                    "Độc giả đang giữ sách quá hạn trả",
                    "/borrows/overdue", overdueCount));
        }
        if (pendingCount > 0) {
            urgent.add(urgentTask("PENDING_ONLINE", "HIGH",
                    pendingCount + " yêu cầu mượn trực tuyến chờ duyệt",
                    "Độc giả đang chờ thủ thư xử lý phiếu",
                    "/borrows/online", pendingCount));
        }
        if (unpaid != null && unpaid.compareTo(BigDecimal.ZERO) > 0) {
            urgent.add(urgentTask("UNPAID_FINE", "MEDIUM",
                    "Phạt chưa thu: " + unpaid.toPlainString() + " đ",
                    "Cần thu hoặc xác nhận thanh toán phạt",
                    "/fines", unpaid));
        }
        m.put("urgentTasks", urgent);
        return m;
    }

    private static Map<String, Object> urgentTask(String type, String priority, String title,
                                                   String description, String href, Object count) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("type", type);
        t.put("priority", priority);
        t.put("title", title);
        t.put("description", description);
        t.put("href", href);
        t.put("count", count);
        return t;
    }

    /** Xu hướng mượn/trả theo ngày. Nếu thiếu dữ liệu thật → demo để dashboard đẹp. */
    public Map<String, Object> borrowTrend(int days) {
        int n = Math.max(7, Math.min(days, 60));
        LocalDate today = LocalDate.now(ZONE);
        LocalDate from = today.minusDays(n - 1L);

        Map<LocalDate, long[]> real = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            real.put(d, new long[]{0, 0});
        }
        borrowSlipItemRepository.findAll().forEach(i -> {
            if (i.getDeletedAt() != null) return;
            if (i.getBorrowedAt() != null) {
                LocalDate d = i.getBorrowedAt().atZone(ZONE).toLocalDate();
                long[] c = real.get(d);
                if (c != null) c[0]++;
            }
            if (i.getReturnedAt() != null) {
                LocalDate d = i.getReturnedAt().atZone(ZONE).toLocalDate();
                long[] c = real.get(d);
                if (c != null) c[1]++;
            }
        });

        long realTotal = real.values().stream().mapToLong(a -> a[0] + a[1]).sum();
        boolean demo = realTotal < 5;
        List<Map<String, Object>> points = new ArrayList<>();
        int i = 0;
        for (var e : real.entrySet()) {
            long borrowed = e.getValue()[0];
            long returned = e.getValue()[1];
            if (demo) {
                // Dao động đẹp quanh 18–32 mượn / 14–28 trả
                borrowed = 18 + ((i * 7 + 3) % 15);
                returned = 14 + ((i * 5 + 1) % 15);
                if (e.getKey().getDayOfWeek().getValue() >= 6) {
                    borrowed = Math.max(8, borrowed - 10);
                    returned = Math.max(6, returned - 8);
                }
            }
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("date", e.getKey().toString());
            p.put("borrowed", borrowed);
            p.put("returned", returned);
            points.add(p);
            i++;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("days", n);
        m.put("demo", demo);
        m.put("points", points);
        return m;
    }

    public Map<String, Object> overdueReport() {
        LocalDate today = LocalDate.now(ZONE);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", borrowSlipItemRepository.findOverdueItems(today).stream().map(i -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId", i.getId());
            row.put("slipCode", i.getSlip().getSlipCode());
            row.put("readerName", i.getSlip().getReader().getUser().getFullName());
            row.put("bookTitle", i.getBook().getTitle());
            row.put("barcode", i.getCopy() != null ? i.getCopy().getBarcode() : null);
            row.put("dueDate", i.getDueDate());
            row.put("overdueDays", today.toEpochDay() - i.getDueDate().toEpochDay());
            return row;
        }).toList());
        return m;
    }

    public Map<String, Object> finesSummary() {
        Map<String, Object> m = new LinkedHashMap<>();
        BigDecimal unpaid = fineRepository.sumAllUnpaid();
        m.put("unpaidTotal", unpaid);
        return m;
    }

    public Map<String, Object> topBooks(int limit) {
        List<Book> books = bookRepository.findAll().stream()
                .filter(b -> b.getDeletedAt() == null)
                .sorted(Comparator.comparingInt(Book::getBorrowCount).reversed())
                .limit(Math.max(1, Math.min(limit, 50)))
                .toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", books.stream().map(b -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", b.getId());
            row.put("title", b.getTitle());
            row.put("borrowCount", b.getBorrowCount());
            return row;
        }).toList());
        return m;
    }

    public Map<String, Object> inventorySummary() {
        Map<String, Object> m = new LinkedHashMap<>();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        bookCopyRepository.findAll().stream()
                .filter(c -> c.getDeletedAt() == null)
                .forEach(c -> byStatus.merge(c.getStatus().name(), 1L, Long::sum));
        m.put("byStatus", byStatus);
        m.put("total", byStatus.values().stream().mapToLong(Long::longValue).sum());
        return m;
    }

    public byte[] exportOverdueExcel() {
        try {
            LocalDate today = LocalDate.now(ZONE);
            List<String> headers = List.of("slipCode", "readerName", "bookTitle", "barcode", "dueDate", "overdueDays");
            List<List<String>> rows = new ArrayList<>();
            borrowSlipItemRepository.findOverdueItems(today).forEach(i -> rows.add(List.of(
                    i.getSlip().getSlipCode(),
                    i.getSlip().getReader().getUser().getFullName(),
                    i.getBook().getTitle(),
                    i.getCopy() != null ? i.getCopy().getBarcode() : "",
                    String.valueOf(i.getDueDate()),
                    String.valueOf(today.toEpochDay() - i.getDueDate().toEpochDay())
            )));
            return ExcelHelper.writeSheet("overdue", headers, rows);
        } catch (Exception ex) {
            throw new BusinessException("EXPORT_FAILED", "Xuất báo cáo thất bại: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
