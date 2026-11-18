package com.library.borrow.service;

import com.library.borrow.dto.ReturnPreviewResponse;
import com.library.borrow.dto.ReturnRequest;
import com.library.borrow.dto.ReturnResultResponse;
import com.library.borrow.entity.BorrowSlip;
import com.library.borrow.entity.BorrowSlipItem;
import com.library.borrow.repository.BorrowSlipItemRepository;
import com.library.borrow.repository.BorrowSlipRepository;
import com.library.catalog.entity.BookCopy;
import com.library.catalog.repository.BookCopyRepository;
import com.library.common.enums.BorrowItemStatus;
import com.library.common.enums.BorrowSlipStatus;
import com.library.common.enums.CopyCondition;
import com.library.common.enums.CopyStatus;
import com.library.common.enums.FineStatus;
import com.library.common.enums.FineType;
import com.library.common.exception.BusinessException;
import com.library.fine.entity.Fine;
import com.library.fine.repository.FineRepository;
import com.library.security.AuthUserPrincipal;
import com.library.security.SecurityUtils;
import com.library.user.entity.User;
import com.library.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnService {

    private final BorrowSlipItemRepository borrowSlipItemRepository;
    private final BorrowSlipRepository borrowSlipRepository;
    private final BookCopyRepository bookCopyRepository;
    private final FineRepository fineRepository;
    private final UserRepository userRepository;
    private final FineCalculator fineCalculator;
    private final ReservationQueueService reservationQueueService;
    private final BorrowMapper borrowMapper;

    @Transactional(readOnly = true)
    public ReturnPreviewResponse preview(String barcode) {
        BorrowSlipItem item = findBorrowingItemByBarcode(barcode);
        LocalDate today = LocalDate.now(BorrowPolicyService.ZONE);
        var overdue = fineCalculator.previewOverdue(item.getDueDate(), today, item.getBook());
        return ReturnPreviewResponse.builder()
                .itemId(item.getId())
                .slipId(item.getSlip().getId())
                .slipCode(item.getSlip().getSlipCode())
                .readerId(item.getSlip().getReader().getId())
                .readerName(item.getSlip().getReader().getUser().getFullName())
                .bookId(item.getBook().getId())
                .bookTitle(item.getBook().getTitle())
                .barcode(item.getCopy().getBarcode())
                .dueDate(item.getDueDate())
                .overdueDays(overdue.overdueDays())
                .estimatedFine(overdue.amount())
                .renewCount(item.getRenewCount())
                .build();
    }

    public ReturnResultResponse confirm(ReturnRequest request) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        User staff = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        Instant now = Instant.now();
        LocalDate today = LocalDate.now(BorrowPolicyService.ZONE);
        List<ReturnResultResponse.FineLine> fineLines = new ArrayList<>();
        List<String> notices = new ArrayList<>();
        BorrowSlip lastSlip = null;
        BigDecimal totalFine = BigDecimal.ZERO;

        for (ReturnRequest.ReturnItemRequest req : request.getItems()) {
            BorrowSlipItem item = borrowSlipItemRepository
                    .findByIdForUpdate(findBorrowingItemByBarcode(req.getCopyBarcode()).getId())
                    .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy item đang mượn", HttpStatus.NOT_FOUND));

            BookCopy copy = bookCopyRepository.findByIdForUpdate(item.getCopy().getId())
                    .orElseThrow(() -> new BusinessException("COPY_NOT_FOUND", "Không tìm thấy bản sao", HttpStatus.NOT_FOUND));

            String conditionCode = req.getCondition() == null ? "NORMAL" : req.getCondition().trim().toUpperCase(Locale.ROOT);
            FineType damageType = null;
            BorrowItemStatus itemStatus = BorrowItemStatus.RETURNED;
            CopyStatus copyStatus;

            switch (conditionCode) {
                case "NORMAL" -> copyStatus = CopyStatus.AVAILABLE;
                case "DAMAGED_MINOR" -> {
                    damageType = FineType.DAMAGED_MINOR;
                    itemStatus = BorrowItemStatus.DAMAGED;
                    copyStatus = CopyStatus.DAMAGED;
                    requireDamageNote(req);
                }
                case "DAMAGED_MAJOR" -> {
                    damageType = FineType.DAMAGED_MAJOR;
                    itemStatus = BorrowItemStatus.DAMAGED;
                    copyStatus = CopyStatus.MAINTENANCE;
                    requireDamageNote(req);
                }
                case "LOST" -> {
                    damageType = FineType.LOST;
                    itemStatus = BorrowItemStatus.LOST;
                    copyStatus = CopyStatus.LOST;
                }
                default -> throw new BusinessException("INVALID_CONDITION", "Tình trạng trả không hợp lệ: " + conditionCode);
            }

            var overdue = fineCalculator.previewOverdue(item.getDueDate(), today, item.getBook());
            BigDecimal overdueAmount = overdue.amount();
            if (req.getFineOverride() != null && damageType == null) {
                overdueAmount = req.getFineOverride();
            }

            if (overdueAmount.compareTo(BigDecimal.ZERO) > 0) {
                Fine fine = createFine(item, FineType.OVERDUE, overdueAmount, overdue.overdueDays(),
                        "Trả trễ " + overdue.overdueDays() + " ngày", staff);
                fineLines.add(line(fine, copy.getBarcode(), item.getBook().getTitle()));
                totalFine = totalFine.add(fine.getAmount());
            }

            if (damageType != null) {
                BigDecimal amount = req.getFineOverride() != null
                        ? req.getFineOverride()
                        : fineCalculator.calculateDamageOrLost(damageType, item.getBook());
                Fine fine = createFine(item, damageType, amount, null, req.getNote(), staff);
                fineLines.add(line(fine, copy.getBarcode(), item.getBook().getTitle()));
                totalFine = totalFine.add(fine.getAmount());
            }

            item.setStatus(itemStatus);
            item.setReturnedAt(now);
            item.setReturnedBy(staff);
            item.setReturnNote(req.getNote());
            item.setDamageImages(req.getImages());
            if (!"LOST".equals(conditionCode) && !"NORMAL".equals(conditionCode)) {
                item.setConditionIn(CopyCondition.POOR);
            } else if ("NORMAL".equals(conditionCode)) {
                item.setConditionIn(copy.getCondition());
            }
            item.setUpdatedBy(staff.getId());
            borrowSlipItemRepository.save(item);

            if (copyStatus == CopyStatus.AVAILABLE) {
                reservationQueueService.assignReturnedCopyToNext(copy).ifPresent(notices::add);
            } else {
                copy.setStatus(copyStatus);
            }
            bookCopyRepository.save(copy);

            lastSlip = updateSlipStatus(item.getSlip());
        }

        List<BorrowSlipItem> items = lastSlip == null
                ? List.of()
                : borrowSlipItemRepository.findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(lastSlip.getId());

        return ReturnResultResponse.builder()
                .slip(lastSlip == null ? null : borrowMapper.toResponse(lastSlip, items))
                .fines(fineLines)
                .totalFine(totalFine)
                .reservationNotices(notices)
                .build();
    }

    public BorrowSlipResponseProxy reportLost(Long itemId, String note) {
        ReturnRequest request = new ReturnRequest();
        BorrowSlipItem item = borrowSlipItemRepository.findById(itemId)
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy item", HttpStatus.NOT_FOUND));
        if (item.getStatus() != BorrowItemStatus.BORROWING || item.getCopy() == null) {
            throw new BusinessException("INVALID_STATUS", "Chỉ báo mất item đang BORROWING");
        }
        ReturnRequest.ReturnItemRequest ri = new ReturnRequest.ReturnItemRequest();
        ri.setCopyBarcode(item.getCopy().getBarcode());
        ri.setCondition("LOST");
        ri.setNote(note);
        request.setItems(List.of(ri));
        ReturnResultResponse result = confirm(request);
        return new BorrowSlipResponseProxy(result);
    }

    private BorrowSlip updateSlipStatus(BorrowSlip slip) {
        long remaining = borrowSlipItemRepository.countBySlipAndStatuses(
                slip.getId(), EnumSet.of(BorrowItemStatus.BORROWING, BorrowItemStatus.HOLD, BorrowItemStatus.REQUESTED));
        if (remaining == 0) {
            slip.setStatus(BorrowSlipStatus.RETURNED);
        } else {
            slip.setStatus(BorrowSlipStatus.PARTIALLY_RETURNED);
        }
        slip.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return borrowSlipRepository.save(slip);
    }

    private Fine createFine(BorrowSlipItem item, FineType type, BigDecimal amount, Integer overdueDays,
                            String reason, User staff) {
        Fine fine = new Fine();
        fine.setReader(item.getSlip().getReader());
        fine.setSlipItem(item);
        fine.setType(type);
        fine.setAmount(amount);
        fine.setPaidAmount(BigDecimal.ZERO);
        fine.setStatus(FineStatus.UNPAID);
        fine.setOverdueDays(overdueDays);
        fine.setReason(reason);
        fine.setCreatedBy(staff.getId());
        fine.setUpdatedBy(staff.getId());
        return fineRepository.save(fine);
    }

    private void requireDamageNote(ReturnRequest.ReturnItemRequest req) {
        if (!StringUtils.hasText(req.getNote())) {
            throw new BusinessException("DAMAGE_NOTE_REQUIRED", "Bắt buộc mô tả khi sách hư hỏng (BR-30)");
        }
    }

    private BorrowSlipItem findBorrowingItemByBarcode(String barcode) {
        if (!StringUtils.hasText(barcode)) {
            throw new BusinessException("INVALID_REQUEST", "Thiếu barcode");
        }
        BookCopy copy = bookCopyRepository.findByBarcodeAndDeletedAtIsNull(barcode.trim())
                .orElseThrow(() -> new BusinessException("COPY_NOT_FOUND", "Không tìm thấy bản sao", HttpStatus.NOT_FOUND));
        return borrowSlipItemRepository
                .findFirstByCopy_IdAndStatusAndDeletedAtIsNull(copy.getId(), BorrowItemStatus.BORROWING)
                .orElseThrow(() -> new BusinessException("NOT_BORROWING", "Bản sao không đang được mượn", HttpStatus.NOT_FOUND));
    }

    private ReturnResultResponse.FineLine line(Fine fine, String barcode, String title) {
        return ReturnResultResponse.FineLine.builder()
                .fineId(fine.getId())
                .type(fine.getType().name())
                .amount(fine.getAmount())
                .barcode(barcode)
                .bookTitle(title)
                .build();
    }

    /** wrapper nhỏ để tái dùng response báo mất */
    public record BorrowSlipResponseProxy(ReturnResultResponse result) {
    }
}
