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
