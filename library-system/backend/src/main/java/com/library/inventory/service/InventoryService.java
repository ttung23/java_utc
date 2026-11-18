package com.library.inventory.service;

import com.library.catalog.entity.Book;
import com.library.catalog.entity.BookCopy;
import com.library.catalog.entity.Location;
import com.library.catalog.repository.BookCopyRepository;
import com.library.catalog.repository.BookRepository;
import com.library.catalog.repository.LocationRepository;
import com.library.catalog.service.BookCopyService;
import com.library.common.enums.AcquisitionSource;
import com.library.common.enums.CopyCondition;
import com.library.common.enums.CopyStatus;
import com.library.common.enums.DisposalStatus;
import com.library.common.enums.ImportReceiptStatus;
import com.library.common.enums.InventoryAuditResult;
import com.library.common.enums.InventoryAuditStatus;
import com.library.common.exception.BusinessException;
import com.library.inventory.entity.DisposalItem;
import com.library.inventory.entity.DisposalReceipt;
import com.library.inventory.entity.ImportItem;
import com.library.inventory.entity.ImportReceipt;
import com.library.inventory.entity.InventoryAudit;
import com.library.inventory.entity.InventoryAuditItem;
import com.library.inventory.entity.Supplier;
import com.library.inventory.repository.DisposalItemRepository;
import com.library.inventory.repository.DisposalReceiptRepository;
import com.library.inventory.repository.ImportItemRepository;
import com.library.inventory.repository.ImportReceiptRepository;
import com.library.inventory.repository.InventoryAuditItemRepository;
import com.library.inventory.repository.InventoryAuditRepository;
import com.library.inventory.repository.SupplierRepository;
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
public class InventoryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SupplierRepository supplierRepository;
    private final ImportReceiptRepository importReceiptRepository;
    private final ImportItemRepository importItemRepository;
    private final InventoryAuditRepository auditRepository;
    private final InventoryAuditItemRepository auditItemRepository;
    private final DisposalReceiptRepository disposalReceiptRepository;
    private final DisposalItemRepository disposalItemRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LocationRepository locationRepository;
    private final BookCopyService bookCopyService;
    private final UserRepository userRepository;

    // ---- Suppliers ----
    public Map<String, Object> createSupplier(Map<String, Object> req) {
        Supplier s = new Supplier();
        applySupplier(s, req);
        s.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        supplierRepository.save(s);
        return supplierMap(s);
    }

    public Map<String, Object> updateSupplier(Long id, Map<String, Object> req) {
        Supplier s = supplierRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy NCC", HttpStatus.NOT_FOUND));
        applySupplier(s, req);
        s.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        supplierRepository.save(s);
        return supplierMap(s);
    }

    public void deleteSupplier(Long id) {
        Supplier s = supplierRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy NCC", HttpStatus.NOT_FOUND));
        s.setDeletedAt(Instant.now());
        s.setActive(false);
        supplierRepository.save(s);
    }

    @Transactional(readOnly = true)
    public PageResult listSuppliers(String q, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("name"));
        Page<Supplier> result = StringUtils.hasText(q)
                ? supplierRepository.findByDeletedAtIsNullAndNameContainingIgnoreCase(q.trim(), pr)
                : supplierRepository.findByDeletedAtIsNull(pr);
        return new PageResult(result.map(this::supplierMap).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    // ---- Imports ----
    public Map<String, Object> createImport(Long supplierId, String source, String note, List<Map<String, Object>> items) {
        ImportReceipt receipt = new ImportReceipt();
        receipt.setReceiptCode(nextCode("PN-", importReceiptRepository.countByCodePrefix("PN-" + today() + "-%") + 0, "PN-"));
        if (supplierId != null) {
            receipt.setSupplier(supplierRepository.findByIdAndDeletedAtIsNull(supplierId).orElse(null));
        }
        receipt.setSource(source != null ? source : "PURCHASE");
        receipt.setStatus(ImportReceiptStatus.DRAFT);
        receipt.setNote(note);
        receipt.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        importReceiptRepository.save(receipt);
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> itemReq : items == null ? List.<Map<String, Object>>of() : items) {
            ImportItem item = new ImportItem();
            item.setReceipt(receipt);
            Long bookId = ((Number) itemReq.get("bookId")).longValue();
            Book book = bookRepository.findByIdAndDeletedAtIsNull(bookId)
                    .orElseThrow(() -> new BusinessException("NOT_FOUND", "Sách không tồn tại: " + bookId, HttpStatus.NOT_FOUND));
            item.setBook(book);
            int qty = ((Number) itemReq.getOrDefault("quantity", 1)).intValue();
            item.setQuantity(qty);
            BigDecimal price = itemReq.get("unitPrice") != null
                    ? new BigDecimal(itemReq.get("unitPrice").toString())
                    : (book.getPrice() != null ? book.getPrice() : BigDecimal.ZERO);
            item.setUnitPrice(price);
            if (itemReq.get("locationId") != null) {
                Long locId = ((Number) itemReq.get("locationId")).longValue();
                item.setLocation(locationRepository.findByIdAndDeletedAtIsNull(locId).orElse(null));
            }
            importItemRepository.save(item);
            total = total.add(price.multiply(BigDecimal.valueOf(qty)));
        }
        receipt.setTotalAmount(total);
        importReceiptRepository.save(receipt);
        return importMap(receipt);
    }

    public Map<String, Object> submitImport(Long id) {
        ImportReceipt receipt = requireImport(id);
        if (receipt.getStatus() != ImportReceiptStatus.DRAFT) {
            throw new BusinessException("INVALID_STATUS", "Chỉ submit DRAFT");
        }
        receipt.setStatus(ImportReceiptStatus.PENDING_APPROVAL);
        importReceiptRepository.save(receipt);
        return importMap(receipt);
    }

    public Map<String, Object> approveImport(Long id) {
        ImportReceipt receipt = requireImport(id);
        if (receipt.getStatus() != ImportReceiptStatus.PENDING_APPROVAL) {
            throw new BusinessException("INVALID_STATUS", "Chỉ duyệt PENDING_APPROVAL");
        }
        User user = userRepository.findById(SecurityUtils.requireCurrentUser().getId()).orElseThrow();
        receipt.setApprovedBy(user);
        receipt.setStatus(ImportReceiptStatus.PENDING_APPROVAL);
        // giữ pending rồi complete riêng, hoặc complete luôn — SPEC: approve rồi complete
        importReceiptRepository.save(receipt);
        return completeImport(id);
    }

    public Map<String, Object> completeImport(Long id) {
        ImportReceipt receipt = requireImport(id);
        if (receipt.getStatus() == ImportReceiptStatus.COMPLETED) {
            return importMap(receipt);
        }
        if (receipt.getStatus() == ImportReceiptStatus.CANCELLED) {
            throw new BusinessException("INVALID_STATUS", "Phiếu đã hủy");
        }
        List<ImportItem> items = importItemRepository.findByReceipt_Id(id);
        for (ImportItem item : items) {
            bookCopyService.createCopies(
                    item.getBook(),
                    item.getQuantity(),
                    item.getLocation(),
                    CopyCondition.NEW,
                    item.getUnitPrice(),
                    AcquisitionSource.PURCHASE
            );
        }
        receipt.setStatus(ImportReceiptStatus.COMPLETED);
        receipt.setCompletedAt(Instant.now());
        if (receipt.getApprovedBy() == null) {
            receipt.setApprovedBy(userRepository.findById(SecurityUtils.requireCurrentUser().getId()).orElse(null));
        }
        importReceiptRepository.save(receipt);
        return importMap(receipt);
    }

    @Transactional(readOnly = true)
    public PageResult listImports(int page, int size) {
        Page<ImportReceipt> result = importReceiptRepository.findByDeletedAtIsNull(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult(result.map(this::importMap).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    // ---- Audits ----
    public Map<String, Object> createAudit(String scopeType, String scopeValue) {
        InventoryAudit audit = new InventoryAudit();
        audit.setCode(nextAuditCode());
        audit.setScopeType(scopeType);
        audit.setScopeValue(scopeValue);
        audit.setStatus(InventoryAuditStatus.OPEN);
        audit.setStartedAt(Instant.now());
        audit.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        auditRepository.save(audit);
        return auditMap(audit);
    }

    public Map<String, Object> scanAudit(Long auditId, String barcode) {
        InventoryAudit audit = requireAudit(auditId);
        if (audit.getStatus() != InventoryAuditStatus.OPEN) {
            throw new BusinessException("INVALID_STATUS", "Đợt kiểm kê đã đóng");
        }
        BookCopy copy = bookCopyRepository.findByBarcodeAndDeletedAtIsNull(barcode.trim())
                .orElseThrow(() -> new BusinessException("COPY_NOT_FOUND", "Không tìm thấy bản sao", HttpStatus.NOT_FOUND));
        InventoryAuditItem item = auditItemRepository.findByAudit_IdAndCopy_Id(auditId, copy.getId())
                .orElseGet(InventoryAuditItem::new);
        item.setAudit(audit);
        item.setCopy(copy);
        item.setExpectedLocation(copy.getLocation());
        item.setFound(true);
        item.setFoundAt(Instant.now());
        item.setScannedBy(userRepository.findById(SecurityUtils.requireCurrentUser().getId()).orElse(null));
        item.setResult(InventoryAuditResult.MATCH);
        auditItemRepository.save(item);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("auditId", auditId);
        m.put("copyId", copy.getId());
        m.put("barcode", copy.getBarcode());
        m.put("result", item.getResult());
        return m;
    }

    public Map<String, Object> closeAudit(Long auditId) {
        InventoryAudit audit = requireAudit(auditId);
        audit.setStatus(InventoryAuditStatus.CLOSED);
        audit.setClosedAt(Instant.now());
        auditRepository.save(audit);
        List<InventoryAuditItem> items = auditItemRepository.findByAudit_Id(auditId);
        Map<String, Object> m = auditMap(audit);
        m.put("items", items.stream().map(i -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("copyId", i.getCopy() != null ? i.getCopy().getId() : null);
            row.put("barcode", i.getCopy() != null ? i.getCopy().getBarcode() : null);
            row.put("found", i.getFound());
            row.put("result", i.getResult());
            return row;
        }).toList());
        return m;
    }

    public Map<String, Object> applyMissingAsLost(Long auditId) {
        InventoryAudit audit = requireAudit(auditId);
        if (audit.getStatus() != InventoryAuditStatus.CLOSED) {
            throw new BusinessException("INVALID_STATUS", "Chỉ điều chỉnh sau khi đóng kiểm kê");
        }
        int updated = 0;
        for (InventoryAuditItem item : auditItemRepository.findByAudit_Id(auditId)) {
            if (item.getResult() == InventoryAuditResult.MISSING && item.getCopy() != null) {
                BookCopy copy = item.getCopy();
                copy.setStatus(CopyStatus.LOST);
                bookCopyRepository.save(copy);
                updated++;
            }
        }
        Map<String, Object> m = auditMap(audit);
        m.put("adjustedToLost", updated);
        return m;
    }

    @Transactional(readOnly = true)
    public PageResult listAudits(int page, int size) {
        Page<InventoryAudit> result = auditRepository.findByDeletedAtIsNull(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult(result.map(this::auditMap).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    // ---- Disposal ----
    public Map<String, Object> createDisposal(String reason, List<Long> copyIds, String note) {
        DisposalReceipt receipt = new DisposalReceipt();
        receipt.setCode(nextDisposalCode());
        receipt.setReason(reason);
        receipt.setNote(note);
        receipt.setStatus(DisposalStatus.PENDING);
        receipt.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        disposalReceiptRepository.save(receipt);
        for (Long copyId : copyIds == null ? List.<Long>of() : copyIds) {
            BookCopy copy = bookCopyRepository.findByIdAndDeletedAtIsNull(copyId)
                    .orElseThrow(() -> new BusinessException("COPY_NOT_FOUND", "Copy không tồn tại: " + copyId, HttpStatus.NOT_FOUND));
            DisposalItem item = new DisposalItem();
            item.setDisposal(receipt);
            item.setCopy(copy);
            disposalItemRepository.save(item);
        }
        return disposalMap(receipt);
    }

    public Map<String, Object> approveDisposal(Long id) {
        DisposalReceipt receipt = disposalReceiptRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy phiếu thanh lý", HttpStatus.NOT_FOUND));
        User admin = userRepository.findById(SecurityUtils.requireCurrentUser().getId()).orElseThrow();
        receipt.setStatus(DisposalStatus.APPROVED);
        receipt.setApprovedBy(admin);
        receipt.setApprovedAt(Instant.now());
        disposalReceiptRepository.save(receipt);
        for (DisposalItem item : disposalItemRepository.findByDisposal_Id(id)) {
            BookCopy copy = item.getCopy();
            copy.setStatus(CopyStatus.WITHDRAWN);
            copy.setWithdrawnAt(Instant.now());
            copy.setWithdrawnReason(receipt.getReason());
            bookCopyRepository.save(copy);
        }
        receipt.setStatus(DisposalStatus.COMPLETED);
        disposalReceiptRepository.save(receipt);
        return disposalMap(receipt);
    }

    @Transactional(readOnly = true)
    public PageResult listDisposals(int page, int size) {
        Page<DisposalReceipt> result = disposalReceiptRepository.findByDeletedAtIsNull(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult(result.map(this::disposalMap).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    // helpers
    private void applySupplier(Supplier s, Map<String, Object> req) {
        if (req.get("name") != null) s.setName(req.get("name").toString());
        if (req.get("contactName") != null) s.setContactName(req.get("contactName").toString());
        if (req.get("phone") != null) s.setPhone(req.get("phone").toString());
        if (req.get("email") != null) s.setEmail(req.get("email").toString());
        if (req.get("address") != null) s.setAddress(req.get("address").toString());
        if (req.get("taxCode") != null) s.setTaxCode(req.get("taxCode").toString());
        if (req.get("note") != null) s.setNote(req.get("note").toString());
        if (req.get("active") != null) s.setActive(Boolean.parseBoolean(req.get("active").toString()));
        if (!StringUtils.hasText(s.getName())) {
            throw new BusinessException("VALIDATION_ERROR", "Tên NCC bắt buộc");
        }
    }

    private ImportReceipt requireImport(Long id) {
        return importReceiptRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy phiếu nhập", HttpStatus.NOT_FOUND));
    }

    private InventoryAudit requireAudit(Long id) {
        return auditRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy đợt kiểm kê", HttpStatus.NOT_FOUND));
    }

    private String today() {
        return LocalDate.now(ZONE).format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String nextCode(String ignored, long ignoredCount, String typePrefix) {
        String day = today();
        String prefix = typePrefix + day + "-";
        long count = importReceiptRepository.countByCodePrefix(prefix + "%");
        return prefix + String.format("%04d", count + 1);
    }

    private String nextAuditCode() {
        String year = String.valueOf(LocalDate.now(ZONE).getYear());
        String prefix = "KK-" + year + "-";
        long count = auditRepository.countByCodePrefix(prefix + "%");
        return prefix + String.format("%02d", count + 1);
    }

    private String nextDisposalCode() {
        String prefix = "TL-" + today() + "-";
        long count = disposalReceiptRepository.countByCodePrefix(prefix + "%");
        return prefix + String.format("%04d", count + 1);
    }

    private Map<String, Object> supplierMap(Supplier s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("contactName", s.getContactName());
        m.put("phone", s.getPhone());
        m.put("email", s.getEmail());
        m.put("address", s.getAddress());
        m.put("taxCode", s.getTaxCode());
        m.put("note", s.getNote());
        m.put("active", s.isActive());
        return m;
    }

    private Map<String, Object> importMap(ImportReceipt r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("receiptCode", r.getReceiptCode());
        m.put("supplierId", r.getSupplier() != null ? r.getSupplier().getId() : null);
        m.put("supplierName", r.getSupplier() != null ? r.getSupplier().getName() : null);
        m.put("source", r.getSource());
        m.put("status", r.getStatus());
        m.put("totalAmount", r.getTotalAmount());
        m.put("note", r.getNote());
        m.put("completedAt", r.getCompletedAt());
        m.put("items", importItemRepository.findByReceipt_Id(r.getId()).stream().map(i -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", i.getId());
            row.put("bookId", i.getBook().getId());
            row.put("bookTitle", i.getBook().getTitle());
            row.put("quantity", i.getQuantity());
            row.put("unitPrice", i.getUnitPrice());
            row.put("locationId", i.getLocation() != null ? i.getLocation().getId() : null);
            return row;
        }).toList());
        return m;
    }

    private Map<String, Object> auditMap(InventoryAudit a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("code", a.getCode());
        m.put("scopeType", a.getScopeType());
        m.put("scopeValue", a.getScopeValue());
        m.put("status", a.getStatus());
        m.put("startedAt", a.getStartedAt());
        m.put("closedAt", a.getClosedAt());
        return m;
    }

    private Map<String, Object> disposalMap(DisposalReceipt d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("code", d.getCode());
        m.put("reason", d.getReason());
        m.put("status", d.getStatus());
        m.put("note", d.getNote());
        m.put("approvedAt", d.getApprovedAt());
        m.put("items", disposalItemRepository.findByDisposal_Id(d.getId()).stream().map(i -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("copyId", i.getCopy().getId());
            row.put("barcode", i.getCopy().getBarcode());
            return row;
        }).toList());
        return m;
    }

    public record PageResult(List<Map<String, Object>> items, int page, int size, long total) {
    }
}
