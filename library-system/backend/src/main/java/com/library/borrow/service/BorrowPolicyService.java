package com.library.borrow.service;

import com.library.borrow.dto.BorrowValidationResult;
import com.library.borrow.dto.PolicyViolation;
import com.library.borrow.repository.BorrowSlipItemRepository;
import com.library.borrow.repository.ReservationRepository;
import com.library.catalog.entity.Book;
import com.library.catalog.entity.BookCopy;
import com.library.common.enums.CardStatus;
import com.library.common.enums.CopyStatus;
import com.library.common.enums.UserStatus;
import com.library.fine.repository.FineRepository;
import com.library.reader.entity.LibraryCard;
import com.library.reader.entity.Reader;
import com.library.reader.entity.ReaderType;
import com.library.reader.repository.LibraryCardRepository;
import com.library.settings.service.LibrarySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checklist SPEC 7.11 / BR-01→BR-12.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BorrowPolicyService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final LibraryCardRepository libraryCardRepository;
    private final BorrowSlipItemRepository borrowSlipItemRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final LibrarySettingsService settingsService;

    public BorrowValidationResult validateBorrow(Reader reader, List<BookCopy> copies) {
        List<PolicyViolation> violations = new ArrayList<>();
        ReaderType type = reader.getReaderType();
        LocalDate today = LocalDate.now(ZONE);

        LibraryCard card = libraryCardRepository
                .findFirstByReader_IdAndStatusAndDeletedAtIsNullOrderByIssuedAtDesc(reader.getId(), CardStatus.ACTIVE)
                .orElse(null);

        if (card == null) {
            violations.add(v("CARD_INACTIVE", "Không có thẻ ACTIVE"));
        } else if (card.getExpiresAt() != null && card.getExpiresAt().isBefore(Instant.now())) {
            violations.add(v("CARD_EXPIRED", "Thẻ đã hết hạn"));
        }

        if (reader.getUser().getStatus() != UserStatus.ACTIVE) {
            violations.add(v("ACCOUNT_LOCKED", "Tài khoản không hoạt động hoặc bị khóa"));
        }
        if (reader.isBlacklisted()) {
            violations.add(v("READER_BLACKLISTED", "Độc giả trong danh sách đen"));
        }

        long overdueCount = borrowSlipItemRepository.countOverdueByReader(reader.getId(), today);
        if (settingsService.blockBorrowIfOverdue() && overdueCount > 0) {
            violations.add(v("READER_HAS_OVERDUE", "Đang có sách quá hạn chưa trả"));
        }

        BigDecimal unpaid = fineRepository.sumUnpaidByReader(reader.getId());
        if (unpaid.compareTo(settingsService.maxUnpaidFine()) > 0) {
            violations.add(v("UNPAID_FINE_LIMIT",
                    "Phạt chưa nộp vượt ngưỡng (" + unpaid.toPlainString() + "đ)"));
        }

        long currentBorrowing = borrowSlipItemRepository.countActiveBorrowingByReader(reader.getId());
        long pendingOnline = borrowSlipItemRepository.countPendingOnlineByReader(reader.getId());
        int requested = copies == null ? 0 : copies.size();
        int maxBooks = type.getMaxBooks();
        long used = currentBorrowing + pendingOnline + requested;
        if (used > maxBooks) {
            violations.add(v("BORROW_LIMIT_EXCEEDED",
                    "Vượt hạn mức mượn (đang mượn " + currentBorrowing
                            + " + chờ online " + pendingOnline
                            + " + yêu cầu mới " + requested + " > " + maxBooks + ")"));
        }

        Set<Long> titlesInRequest = new HashSet<>();
        Set<Long> barcodesInRequest = new HashSet<>();
        Set<Long> currentlyBorrowedBookIds = new HashSet<>(
                borrowSlipItemRepository.findBorrowedBookIdsByReader(reader.getId())
        );

        if (copies != null) {
            for (BookCopy copy : copies) {
                if (copy == null) {
                    violations.add(v("COPY_NOT_FOUND", "Bản sao không tồn tại"));
                    continue;
                }
                Book book = copy.getBook();
                if (!barcodesInRequest.add(copy.getId())) {
                    violations.add(v("DUPLICATE_COPY", "Trùng bản sao trong phiếu: " + copy.getBarcode()));
                }
                if (book != null && !titlesInRequest.add(book.getId())) {
                    violations.add(v("DUPLICATE_TITLE", "Không được mượn trùng đầu sách trong cùng phiếu"));
                }
                if (book != null && currentlyBorrowedBookIds.contains(book.getId())) {
                    violations.add(v("DUPLICATE_TITLE",
                            "Đang mượn đầu sách này rồi: " + book.getTitle()));
                }
                if (book != null && book.isReference()) {
                    violations.add(v("REFERENCE_ONLY", "Sách chỉ đọc tại chỗ: " + book.getTitle()));
                }
                if (book != null && !book.isLoanable()) {
                    violations.add(v("NOT_LOANABLE", "Đầu sách không cho mượn: " + book.getTitle()));
                }
                if (copy.getStatus() == CopyStatus.AVAILABLE) {
                    // ok
                } else if (copy.getStatus() == CopyStatus.HOLD) {
                    boolean holdForReader = borrowSlipItemRepository.existsHoldForReaderAndCopy(
                            reader.getId(), copy.getId())
                            || reservationRepository.existsReadyHoldForReaderAndCopy(
                                    reader.getId(), copy.getId());
                    if (!holdForReader) {
                        violations.add(v("COPY_ON_HOLD",
                                "Bản sao đang giữ cho người khác: " + copy.getBarcode()));
                    }
                } else {
                    violations.add(v("COPY_NOT_AVAILABLE",
                            "Bản sao không sẵn sàng (" + copy.getStatus() + "): " + copy.getBarcode()));
                }
            }
        }

        int remaining = (int) Math.max(0, maxBooks - currentBorrowing - pendingOnline);
        return BorrowValidationResult.builder()
                .valid(violations.isEmpty())
                .remainingQuota(remaining)
                .violations(violations)
                .build();
    }

    public BorrowValidationResult validateOnlineBooks(Reader reader, List<Book> books) {
        List<BookCopy> placeholders = new ArrayList<>();
        if (books != null) {
            for (Book book : books) {
                BookCopy fake = new BookCopy();
                fake.setBook(book);
                fake.setStatus(CopyStatus.AVAILABLE);
                fake.setBarcode("ONLINE-" + book.getId());
                fake.setId(book.getId()); // dùng để check trùng trong phiếu
                placeholders.add(fake);
            }
        }
        // Online: bỏ qua check copy status (chưa gán), chỉ kiểm reader + hạn mức + trùng đầu sách + loanable
        BorrowValidationResult base = validateReaderOnly(reader, books == null ? 0 : books.size());
        List<PolicyViolation> violations = new ArrayList<>(base.getViolations());
        Set<Long> titles = new HashSet<>();
        Set<Long> borrowed = new HashSet<>(borrowSlipItemRepository.findBorrowedBookIdsByReader(reader.getId()));
        Set<Long> pendingBooks = new HashSet<>(borrowSlipItemRepository.findPendingOnlineBookIdsByReader(reader.getId()));

        if (books != null) {
            for (Book book : books) {
                if (!titles.add(book.getId())) {
                    violations.add(v("DUPLICATE_TITLE", "Trùng đầu sách trong yêu cầu"));
                }
                if (borrowed.contains(book.getId()) || pendingBooks.contains(book.getId())) {
                    violations.add(v("DUPLICATE_TITLE", "Đang mượn/chờ đầu sách: " + book.getTitle()));
                }
                if (book.isReference()) {
                    violations.add(v("REFERENCE_ONLY", "Sách chỉ đọc tại chỗ: " + book.getTitle()));
                }
                if (!book.isLoanable()) {
                    violations.add(v("NOT_LOANABLE", "Đầu sách không cho mượn: " + book.getTitle()));
                }
            }
        }
        if (!settingsService.allowOnlineBorrow()) {
            violations.add(v("ONLINE_BORROW_DISABLED", "Hệ thống tạm khóa mượn online"));
        }
        return BorrowValidationResult.builder()
                .valid(violations.isEmpty())
                .remainingQuota(base.getRemainingQuota())
                .violations(violations)
                .build();
    }

    public BorrowValidationResult validateReaderOnly(Reader reader, int additionalItems) {
        List<BookCopy> emptyCopies = new ArrayList<>();
        for (int i = 0; i < additionalItems; i++) {
            BookCopy c = new BookCopy();
            c.setId((long) -(i + 1));
            c.setBarcode("TMP-" + i);
            c.setStatus(CopyStatus.AVAILABLE);
            Book b = new Book();
            b.setId((long) -(i + 1));
            b.setTitle("tmp");
            b.setLoanable(true);
            b.setReference(false);
            c.setBook(b);
            emptyCopies.add(c);
        }
        return validateBorrow(reader, emptyCopies);
    }

    private PolicyViolation v(String code, String message) {
        return PolicyViolation.builder().code(code).message(message).build();
    }
}
