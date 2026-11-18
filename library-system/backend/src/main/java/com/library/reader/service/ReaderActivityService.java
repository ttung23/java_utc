package com.library.reader.service;

import com.library.borrow.repository.BorrowSlipItemRepository;
import com.library.borrow.repository.BorrowSlipRepository;
import com.library.borrow.repository.ReservationRepository;
import com.library.borrow.service.BorrowMapper;
import com.library.borrow.service.BorrowPolicyService;
import com.library.common.enums.BorrowItemStatus;
import com.library.common.exception.BusinessException;
import com.library.fine.repository.FineRepository;
import com.library.reader.entity.Reader;
import com.library.reader.repository.ReaderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReaderActivityService {

    private final ReaderRepository readerRepository;
    private final BorrowSlipItemRepository borrowSlipItemRepository;
    private final BorrowSlipRepository borrowSlipRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final BorrowMapper borrowMapper;

    public List<Map<String, Object>> borrowing(Long readerId) {
        requireReader(readerId);
        LocalDate today = LocalDate.now(BorrowPolicyService.ZONE);
        return borrowSlipItemRepository.findAll().stream()
                .filter(i -> i.getDeletedAt() == null)
                .filter(i -> i.getSlip().getReader().getId().equals(readerId))
                .filter(i -> i.getStatus() == BorrowItemStatus.BORROWING)
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("itemId", i.getId());
                    m.put("slipId", i.getSlip().getId());
                    m.put("slipCode", i.getSlip().getSlipCode());
                    m.put("bookId", i.getBook().getId());
                    m.put("bookTitle", i.getBook().getTitle());
                    m.put("barcode", i.getCopy() != null ? i.getCopy().getBarcode() : null);
                    m.put("dueDate", i.getDueDate());
                    m.put("overdue", i.getDueDate() != null && i.getDueDate().isBefore(today));
                    m.put("renewCount", i.getRenewCount());
                    return m;
                }).toList();
    }

    public List<Map<String, Object>> history(Long readerId) {
        requireReader(readerId);
        return borrowSlipRepository.findByReader_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        readerId, PageRequest.of(0, 50))
                .map(slip -> borrowMapper.toResponse(
                        slip,
                        borrowSlipItemRepository.findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(slip.getId())))
                .map(resp -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", resp.getId());
                    m.put("slipCode", resp.getSlipCode());
                    m.put("status", resp.getStatus());
                    m.put("channel", resp.getChannel());
                    m.put("borrowedAt", resp.getBorrowedAt());
                    m.put("itemCount", resp.getItems().size());
                    return m;
                }).getContent();
    }

    public List<Map<String, Object>> fines(Long readerId) {
        requireReader(readerId);
        return fineRepository.findByReader_IdAndDeletedAtIsNull(readerId, PageRequest.of(0, 100))
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", f.getId());
                    m.put("type", f.getType());
                    m.put("amount", f.getAmount());
                    m.put("paidAmount", f.getPaidAmount());
                    m.put("status", f.getStatus());
                    m.put("createdAt", f.getCreatedAt());
                    return m;
                }).getContent();
    }

    public List<Map<String, Object>> reservations(Long readerId) {
        requireReader(readerId);
        return reservationRepository.findByReader_IdAndDeletedAtIsNull(readerId, PageRequest.of(0, 50))
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("bookId", r.getBook().getId());
                    m.put("bookTitle", r.getBook().getTitle());
                    m.put("status", r.getStatus());
                    m.put("queuePosition", r.getQueuePosition());
                    m.put("readyUntil", r.getReadyUntil());
                    m.put("createdAt", r.getCreatedAt());
                    return m;
                }).getContent();
    }

    private Reader requireReader(Long id) {
        return readerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy độc giả", HttpStatus.NOT_FOUND));
    }
}
