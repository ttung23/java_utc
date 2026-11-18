package com.library.borrow;

import com.library.borrow.repository.BorrowSlipItemRepository;
import com.library.borrow.service.BorrowPolicyService;
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
import com.library.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowPolicyServiceTest {

    @Mock
    LibraryCardRepository libraryCardRepository;
    @Mock
    BorrowSlipItemRepository borrowSlipItemRepository;
    @Mock
    FineRepository fineRepository;
    @Mock
    LibrarySettingsService settingsService;

    @InjectMocks
    BorrowPolicyService policyService;

    Reader reader;
    BookCopy availableCopy;

    @BeforeEach
    void setUp() {
        ReaderType type = new ReaderType();
        type.setMaxBooks(2);
        type.setLoanDays(14);

        User user = new User();
        user.setStatus(UserStatus.ACTIVE);

        reader = new Reader();
        reader.setId(1L);
        reader.setReaderType(type);
        reader.setUser(user);
        reader.setBlacklisted(false);

        LibraryCard card = new LibraryCard();
        card.setStatus(CardStatus.ACTIVE);
        card.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        when(libraryCardRepository.findFirstByReader_IdAndStatusAndDeletedAtIsNullOrderByIssuedAtDesc(eq(1L), eq(CardStatus.ACTIVE)))
                .thenReturn(Optional.of(card));

        when(settingsService.blockBorrowIfOverdue()).thenReturn(true);
        when(settingsService.maxUnpaidFine()).thenReturn(BigDecimal.valueOf(50000));
        when(borrowSlipItemRepository.countOverdueByReader(anyLong(), any())).thenReturn(0L);
        when(borrowSlipItemRepository.countActiveBorrowingByReader(1L)).thenReturn(0L);
        when(borrowSlipItemRepository.countPendingOnlineByReader(1L)).thenReturn(0L);
        when(borrowSlipItemRepository.findBorrowedBookIdsByReader(1L)).thenReturn(List.of());
        when(fineRepository.sumUnpaidByReader(1L)).thenReturn(BigDecimal.ZERO);

        Book book = new Book();
        book.setId(10L);
        book.setTitle("Java");
        book.setLoanable(true);
        book.setReference(false);

        availableCopy = new BookCopy();
        availableCopy.setId(100L);
        availableCopy.setBarcode("BC-1");
        availableCopy.setStatus(CopyStatus.AVAILABLE);
        availableCopy.setBook(book);
    }

    @Test
    void validWhenAllOk() {
        var result = policyService.validateBorrow(reader, List.of(availableCopy));
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void rejectsWhenHasOverdue() {
        when(borrowSlipItemRepository.countOverdueByReader(anyLong(), any())).thenReturn(1L);
        var result = policyService.validateBorrow(reader, List.of(availableCopy));
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> "READER_HAS_OVERDUE".equals(v.getCode()));
    }

    @Test
    void rejectsDuplicateTitle() {
        BookCopy copy2 = new BookCopy();
        copy2.setId(101L);
        copy2.setBarcode("BC-2");
        copy2.setStatus(CopyStatus.AVAILABLE);
        copy2.setBook(availableCopy.getBook());

        var result = policyService.validateBorrow(reader, List.of(availableCopy, copy2));
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> "DUPLICATE_TITLE".equals(v.getCode()));
    }

    @Test
    void rejectsReferenceBook() {
        availableCopy.getBook().setReference(true);
        var result = policyService.validateBorrow(reader, List.of(availableCopy));
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> "REFERENCE_ONLY".equals(v.getCode()));
    }

    @Test
    void rejectsLimitExceeded() {
        when(borrowSlipItemRepository.countActiveBorrowingByReader(1L)).thenReturn(2L);
        var result = policyService.validateBorrow(reader, List.of(availableCopy));
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).anyMatch(v -> "BORROW_LIMIT_EXCEEDED".equals(v.getCode()));
    }
}
