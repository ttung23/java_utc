package com.library.borrow.service;

import com.library.borrow.dto.PolicyViolation;
import com.library.borrow.dto.RenewEligibilityResponse;
import com.library.borrow.entity.BorrowSlipItem;
import com.library.borrow.entity.Renewal;
import com.library.borrow.repository.BorrowSlipItemRepository;
import com.library.borrow.repository.RenewalRepository;
import com.library.borrow.repository.ReservationRepository;
import com.library.common.enums.BorrowChannel;
import com.library.common.enums.BorrowItemStatus;
import com.library.common.enums.CardStatus;
import com.library.common.enums.UserRole;
import com.library.common.exception.BusinessException;
import com.library.fine.repository.FineRepository;
import com.library.reader.entity.LibraryCard;
import com.library.reader.entity.Reader;
import com.library.reader.repository.LibraryCardRepository;
import com.library.reader.repository.ReaderRepository;
import com.library.security.AuthUserPrincipal;
import com.library.security.SecurityUtils;
import com.library.settings.service.LibrarySettingsService;
import com.library.user.entity.User;
import com.library.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class RenewalService {

    private final BorrowSlipItemRepository borrowSlipItemRepository;
    private final RenewalRepository renewalRepository;
    private final ReservationRepository reservationRepository;
    private final LibraryCardRepository libraryCardRepository;
    private final FineRepository fineRepository;
    private final ReaderRepository readerRepository;
    private final UserRepository userRepository;
    private final DueDateCalculator dueDateCalculator;
    private final LibrarySettingsService settingsService;
    private final BorrowMapper borrowMapper;

    @Transactional(readOnly = true)
    public RenewEligibilityResponse eligibility(Long itemId) {
        BorrowSlipItem item = requireBorrowingItem(itemId);
        return check(item, SecurityUtils.requireCurrentUser());
    }

    public BorrowSlipResponseRenew renew(Long itemId, String note) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        BorrowSlipItem item = borrowSlipItemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy item", HttpStatus.NOT_FOUND));
        if (item.getStatus() != BorrowItemStatus.BORROWING) {
            throw new BusinessException("INVALID_STATUS", "Chỉ gia hạn item đang BORROWING");
        }
        assertCanRenew(item, principal);

        RenewEligibilityResponse check = check(item, principal);
        if (!check.isEligible()) {
            throw new BusinessException("RENEW_NOT_ALLOWED", "Không đủ điều kiện gia hạn",
                    HttpStatus.BAD_REQUEST, check.getReasons());
        }

        LocalDate oldDue = item.getDueDate();
        LocalDate newDue = check.getNewDueDate();
        item.setDueDate(newDue);
        item.setRenewCount(item.getRenewCount() + 1);
        item.setUpdatedBy(principal.getId());
        borrowSlipItemRepository.save(item);

        User actor = userRepository.findById(principal.getId()).orElse(null);
        Renewal renewal = new Renewal();
        renewal.setItem(item);
        renewal.setOldDueDate(oldDue);
        renewal.setNewDueDate(newDue);
        renewal.setChannel(principal.getRole() == UserRole.USER ? BorrowChannel.ONLINE : BorrowChannel.COUNTER);
        renewal.setPerformedBy(actor);
        renewal.setNote(note);
        renewalRepository.save(renewal);

        var slip = item.getSlip();
        var items = borrowSlipItemRepository.findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(slip.getId());
        return new BorrowSlipResponseRenew(borrowMapper.toResponse(slip, items));
    }

    private RenewEligibilityResponse check(BorrowSlipItem item, AuthUserPrincipal principal) {
        List<PolicyViolation> reasons = new ArrayList<>();
        Reader reader = item.getSlip().getReader();
        LocalDate today = LocalDate.now(BorrowPolicyService.ZONE);
        LocalDate currentDue = item.getDueDate();
        int maxRenewals = reader.getReaderType().getMaxRenewals();
        int renewDays = reader.getReaderType().getRenewDays();

        if (principal.getRole() == UserRole.USER && !settingsService.allowSelfRenew()) {
            reasons.add(v("SELF_RENEW_DISABLED", "Hệ thống tạm khóa tự gia hạn"));
        }
        if (currentDue != null && currentDue.isBefore(today)) {
            reasons.add(v("ITEM_OVERDUE", "Không gia hạn sách đã quá hạn"));
        }
        if (item.getRenewCount() >= maxRenewals) {
            reasons.add(v("RENEW_LIMIT", "Đã hết số lần gia hạn (" + maxRenewals + ")"));
        }
        if (reservationRepository.existsActiveQueueForBook(item.getBook().getId())) {
            reasons.add(v("HAS_RESERVATION", "Sách đang có người đặt trước"));
        }
        BigDecimal unpaid = fineRepository.sumUnpaidByReader(reader.getId());
        if (unpaid.compareTo(settingsService.maxUnpaidFine()) > 0) {
            reasons.add(v("UNPAID_FINE_LIMIT", "Phạt chưa nộp vượt ngưỡng"));
        }

        LocalDate newDue = currentDue == null ? null : dueDateCalculator.extend(currentDue, renewDays);
        LibraryCard card = libraryCardRepository
                .findFirstByReader_IdAndStatusAndDeletedAtIsNullOrderByIssuedAtDesc(reader.getId(), CardStatus.ACTIVE)
                .orElse(null);
        if (card == null) {
            reasons.add(v("CARD_INACTIVE", "Không có thẻ ACTIVE"));
        } else if (card.getExpiresAt() != null && newDue != null) {
            Instant cardExpiry = card.getExpiresAt();
            Instant newDueInstant = newDue.plusDays(1).atStartOfDay(BorrowPolicyService.ZONE).toInstant();
            if (cardExpiry.isBefore(newDueInstant)) {
                reasons.add(v("CARD_EXPIRES_BEFORE_NEW_DUE", "Thẻ hết hạn trước ngày hạn mới"));
            }
        }

        return RenewEligibilityResponse.builder()
                .eligible(reasons.isEmpty())
                .currentDueDate(currentDue)
                .newDueDate(newDue)
                .renewCount(item.getRenewCount())
                .maxRenewals(maxRenewals)
                .reasons(reasons)
                .build();
    }

    private void assertCanRenew(BorrowSlipItem item, AuthUserPrincipal principal) {
        if (principal.getRole() == UserRole.ADMIN || principal.getRole() == UserRole.STAFF) {
            return;
        }
        Reader me = readerRepository.findByUserIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy hồ sơ độc giả", HttpStatus.NOT_FOUND));
        if (!Objects.equals(me.getId(), item.getSlip().getReader().getId())) {
            throw new BusinessException("FORBIDDEN", "Không được gia hạn phiếu của người khác", HttpStatus.FORBIDDEN);
        }
    }

    private BorrowSlipItem requireBorrowingItem(Long itemId) {
        BorrowSlipItem item = borrowSlipItemRepository.findById(itemId)
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy item", HttpStatus.NOT_FOUND));
        if (item.getStatus() != BorrowItemStatus.BORROWING) {
            throw new BusinessException("INVALID_STATUS", "Item không ở trạng thái BORROWING");
        }
        return item;
    }

    private PolicyViolation v(String code, String message) {
        return PolicyViolation.builder().code(code).message(message).build();
    }

    public record BorrowSlipResponseRenew(com.library.borrow.dto.BorrowSlipResponse slip) {
    }
}
