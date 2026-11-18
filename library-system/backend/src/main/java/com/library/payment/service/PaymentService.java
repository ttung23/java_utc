package com.library.payment.service;

import com.library.common.enums.FineStatus;
import com.library.common.enums.PaymentMethod;
import com.library.common.enums.PaymentStatus;
import com.library.common.enums.UserRole;
import com.library.common.exception.BusinessException;
import com.library.fine.entity.Fine;
import com.library.fine.repository.FineRepository;
import com.library.payment.entity.Payment;
import com.library.payment.entity.PaymentAllocation;
import com.library.payment.repository.PaymentAllocationRepository;
import com.library.payment.repository.PaymentRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final FineRepository fineRepository;
    private final ReaderRepository readerRepository;
    private final UserRepository userRepository;

    public Map<String, Object> collect(Long readerId, List<Long> fineIds, BigDecimal amount, PaymentMethod method, String note) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        User staff = userRepository.findById(principal.getId()).orElseThrow();
        Reader reader = readerRepository.findByIdAndDeletedAtIsNull(readerId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy độc giả", HttpStatus.NOT_FOUND));

        List<Fine> fines = fineRepository.findByIdInAndDeletedAtIsNull(fineIds);
        if (fines.isEmpty()) {
            throw new BusinessException("FINE_NOT_FOUND", "Không có khoản phạt hợp lệ");
        }
        for (Fine fine : fines) {
            if (!fine.getReader().getId().equals(readerId)) {
                throw new BusinessException("FINE_READER_MISMATCH", "Khoản phạt không thuộc độc giả");
            }
            if (fine.getStatus() == FineStatus.PAID || fine.getStatus() == FineStatus.WAIVED) {
                throw new BusinessException("FINE_ALREADY_CLOSED", "Khoản phạt đã đóng: " + fine.getId());
            }
        }

        BigDecimal remainingTotal = fines.stream()
                .map(f -> f.getAmount().subtract(f.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            amount = remainingTotal;
        }
        if (amount.compareTo(remainingTotal) > 0) {
            throw new BusinessException("AMOUNT_EXCEEDS", "Số tiền thu vượt tổng nợ");
        }

        Payment payment = new Payment();
        payment.setReceiptCode(nextReceiptCode());
        payment.setReader(reader);
        payment.setAmount(amount);
        payment.setMethod(method != null ? method : PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setReceivedBy(staff);
        payment.setPaidAt(Instant.now());
        payment.setNote(note);
        payment.setCreatedBy(staff.getId());
        payment.setUpdatedBy(staff.getId());
        paymentRepository.save(payment);

        BigDecimal left = amount;
        List<Map<String, Object>> allocations = new ArrayList<>();
        for (Fine fine : fines) {
            if (left.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal due = fine.getAmount().subtract(fine.getPaidAmount());
            BigDecimal apply = due.min(left);
            fine.setPaidAmount(fine.getPaidAmount().add(apply));
            if (fine.getPaidAmount().compareTo(fine.getAmount()) >= 0) {
                fine.setStatus(FineStatus.PAID);
            } else {
                fine.setStatus(FineStatus.PARTIAL);
            }
            fine.setUpdatedBy(staff.getId());
            fineRepository.save(fine);

            PaymentAllocation allocation = new PaymentAllocation();
            allocation.setPayment(payment);
            allocation.setFine(fine);
            allocation.setAmount(apply);
            allocationRepository.save(allocation);

            Map<String, Object> line = new LinkedHashMap<>();
            line.put("fineId", fine.getId());
            line.put("amount", apply);
            allocations.add(line);
            left = left.subtract(apply);
        }
        Map<String, Object> result = toMap(payment);
        result.put("allocations", allocations);
        return result;
    }

    public Map<String, Object> requestPayment(List<Long> fineIds, BigDecimal amount, String note) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        Reader reader = readerRepository.findByUserIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy hồ sơ độc giả", HttpStatus.NOT_FOUND));
        User user = userRepository.findById(principal.getId()).orElseThrow();

        Payment payment = new Payment();
        payment.setReceiptCode(nextReceiptCode());
        payment.setReader(reader);
        payment.setAmount(amount != null ? amount : BigDecimal.ZERO);
        payment.setMethod(PaymentMethod.ONLINE);
        payment.setStatus(PaymentStatus.PENDING_CONFIRM);
        payment.setNote(note);
        payment.setCreatedBy(user.getId());
        payment.setUpdatedBy(user.getId());
        paymentRepository.save(payment);

        if (fineIds != null) {
            for (Long fineId : fineIds) {
                Fine fine = fineRepository.findByIdAndDeletedAtIsNull(fineId).orElse(null);
                if (fine == null || !fine.getReader().getId().equals(reader.getId())) {
                    continue;
                }
                PaymentAllocation allocation = new PaymentAllocation();
                allocation.setPayment(payment);
                allocation.setFine(fine);
                allocation.setAmount(BigDecimal.ZERO);
                allocationRepository.save(allocation);
            }
        }
        return toMap(payment);
    }

    public Map<String, Object> confirm(Long id) {
        Payment payment = require(id);
        if (payment.getStatus() != PaymentStatus.PENDING_CONFIRM) {
            throw new BusinessException("INVALID_STATUS", "Chỉ xác nhận payment PENDING_CONFIRM");
        }
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        User staff = userRepository.findById(principal.getId()).orElseThrow();
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setReceivedBy(staff);
        payment.setPaidAt(Instant.now());
        payment.setUpdatedBy(staff.getId());
        paymentRepository.save(payment);

        List<PaymentAllocation> allocations = allocationRepository.findByPayment_Id(id);
        BigDecimal left = payment.getAmount();
        for (PaymentAllocation allocation : allocations) {
            Fine fine = allocation.getFine();
            BigDecimal due = fine.getAmount().subtract(fine.getPaidAmount());
            BigDecimal apply = due.min(left);
            fine.setPaidAmount(fine.getPaidAmount().add(apply));
            fine.setStatus(fine.getPaidAmount().compareTo(fine.getAmount()) >= 0 ? FineStatus.PAID : FineStatus.PARTIAL);
            fineRepository.save(fine);
            allocation.setAmount(apply);
            allocationRepository.save(allocation);
            left = left.subtract(apply);
        }
        return toMap(payment);
    }

    public Map<String, Object> refund(Long id, String note) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        if (principal.getRole() != UserRole.ADMIN) {
            throw new BusinessException("FORBIDDEN", "Chỉ ADMIN hoàn tiền", HttpStatus.FORBIDDEN);
        }
        Payment payment = require(id);
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException("INVALID_STATUS", "Chỉ hoàn payment COMPLETED");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setNote((payment.getNote() == null ? "" : payment.getNote() + " | ") + "REFUND: " + note);
        payment.setUpdatedBy(principal.getId());
        paymentRepository.save(payment);
        return toMap(payment);
    }

    @Transactional(readOnly = true)
    public PageResult list(Long readerId, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> result = readerId != null
                ? paymentRepository.findByReader_IdAndDeletedAtIsNull(readerId, pr)
                : paymentRepository.findByDeletedAtIsNull(pr);
        return new PageResult(result.map(this::toMap).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        return toMap(require(id));
    }

    private synchronized String nextReceiptCode() {
        String day = LocalDate.now(ZONE).format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "BL-" + day + "-";
        long count = paymentRepository.countByReceiptPrefix(prefix + "%");
        return prefix + String.format("%04d", count + 1);
    }

    private Payment require(Long id) {
        return paymentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy biên lai", HttpStatus.NOT_FOUND));
    }

    private Map<String, Object> toMap(Payment p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("receiptCode", p.getReceiptCode());
        m.put("readerId", p.getReader().getId());
        m.put("readerName", p.getReader().getUser().getFullName());
        m.put("amount", p.getAmount());
        m.put("method", p.getMethod());
        m.put("status", p.getStatus());
        m.put("paidAt", p.getPaidAt());
        m.put("note", p.getNote());
        m.put("createdAt", p.getCreatedAt());
        return m;
    }

    public record PageResult(List<Map<String, Object>> items, int page, int size, long total) {
    }
}
