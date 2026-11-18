package com.library.fine.service;

import com.library.common.enums.FineStatus;
import com.library.common.enums.FineType;
import com.library.common.enums.UserRole;
import com.library.common.exception.BusinessException;
import com.library.fine.entity.Fine;
import com.library.fine.repository.FineRepository;
import com.library.reader.entity.Reader;
import com.library.reader.repository.ReaderRepository;
import com.library.security.AuthUserPrincipal;
import com.library.security.SecurityUtils;
import com.library.user.entity.User;
import com.library.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class FineService {

    private final FineRepository fineRepository;
    private final ReaderRepository readerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResult list(Long readerId, FineStatus status, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Fine> result;
        if (readerId != null && status != null) {
            result = fineRepository.findByReader_IdAndStatusInAndDeletedAtIsNull(readerId, List.of(status), pr);
        } else if (readerId != null) {
            result = fineRepository.findByReader_IdAndDeletedAtIsNull(readerId, pr);
        } else {
            result = fineRepository.findByDeletedAtIsNull(pr);
        }
        return mapPage(result);
    }

    @Transactional(readOnly = true)
    public PageResult listMine(int page, int size) {
        Reader me = requireMyReader();
        return list(me.getId(), null, page, size);
    }

    public Map<String, Object> createManual(Long readerId, FineType type, BigDecimal amount, String reason) {
        Reader reader = readerRepository.findByIdAndDeletedAtIsNull(readerId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy độc giả", HttpStatus.NOT_FOUND));
        Fine fine = new Fine();
        fine.setReader(reader);
        fine.setType(type != null ? type : FineType.OTHER);
        fine.setAmount(amount);
        fine.setPaidAmount(BigDecimal.ZERO);
        fine.setStatus(FineStatus.UNPAID);
        fine.setReason(reason);
        fine.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        fine.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        fineRepository.save(fine);
        return toMap(fine);
    }

    public Map<String, Object> waive(Long id, String reason, BigDecimal amount) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        if (principal.getRole() != UserRole.ADMIN) {
            throw new BusinessException("FORBIDDEN", "Chỉ ADMIN được miễn/giảm phạt", HttpStatus.FORBIDDEN);
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("WAIVE_REASON_REQUIRED", "Bắt buộc nhập lý do miễn/giảm");
        }
        Fine fine = require(id);
        if (fine.getStatus() == FineStatus.PAID || fine.getStatus() == FineStatus.WAIVED) {
            throw new BusinessException("INVALID_STATUS", "Không thể miễn khoản đã PAID/WAIVED");
        }
        User admin = userRepository.findById(principal.getId()).orElseThrow();
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(fine.getAmount()) < 0) {
            fine.setAmount(amount);
            fine.setStatus(FineStatus.REDUCED);
        } else {
            fine.setStatus(FineStatus.WAIVED);
            fine.setPaidAmount(fine.getAmount());
        }
        fine.setWaivedBy(admin);
        fine.setWaivedReason(reason.trim());
        fine.setUpdatedBy(principal.getId());
        fineRepository.save(fine);
        return toMap(fine);
    }

    private Fine require(Long id) {
        return fineRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy khoản phạt", HttpStatus.NOT_FOUND));
    }

    private Reader requireMyReader() {
        return readerRepository.findByUserIdAndDeletedAtIsNull(SecurityUtils.requireCurrentUser().getId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy hồ sơ độc giả", HttpStatus.NOT_FOUND));
    }

    private PageResult mapPage(Page<Fine> result) {
        return new PageResult(result.map(this::toMap).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private Map<String, Object> toMap(Fine f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.getId());
        m.put("readerId", f.getReader().getId());
        m.put("readerName", f.getReader().getUser().getFullName());
        m.put("slipItemId", f.getSlipItem() != null ? f.getSlipItem().getId() : null);
        m.put("type", f.getType());
        m.put("amount", f.getAmount());
        m.put("paidAmount", f.getPaidAmount());
        m.put("remaining", f.getAmount().subtract(f.getPaidAmount()));
        m.put("status", f.getStatus());
        m.put("overdueDays", f.getOverdueDays());
        m.put("reason", f.getReason());
        m.put("waivedReason", f.getWaivedReason());
        m.put("createdAt", f.getCreatedAt());
        return m;
    }

    public record PageResult(List<Map<String, Object>> items, int page, int size, long total) {
    }
}
