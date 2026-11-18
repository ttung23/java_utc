package com.library.catalog.service;

import com.library.catalog.dto.AddCopiesRequest;
import com.library.catalog.dto.BookCopyResponse;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.dto.UpdateCopyRequest;
import com.library.catalog.entity.Book;
import com.library.catalog.entity.BookCopy;
import com.library.catalog.entity.Location;
import com.library.catalog.repository.BookCopyRepository;
import com.library.catalog.repository.BookCopySpecifications;
import com.library.catalog.repository.BookRepository;
import com.library.catalog.repository.LocationRepository;
import com.library.catalog.support.CatalogPageSupport;
import com.library.common.enums.AcquisitionSource;
import com.library.common.enums.CopyCondition;
import com.library.common.enums.CopyStatus;
import com.library.common.exception.BusinessException;
import com.library.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookCopyService {

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final LocationRepository locationRepository;

    @Transactional(readOnly = true)
    public List<BookCopyResponse> listByBook(Long bookId) {
        requireActiveBook(bookId);
        return bookCopyRepository.findByBook_IdAndDeletedAtIsNullOrderByCopyNoAsc(bookId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BookCopyResponse> addCopies(Long bookId, AddCopiesRequest request) {
        Book book = requireActiveBook(bookId);
        Location location = resolveLocation(request.getLocationId());
        CopyCondition condition = request.getCondition() != null ? request.getCondition() : CopyCondition.GOOD;
        AcquisitionSource source = request.getAcquisitionSource();
        BigDecimal price = request.getPurchasePrice() != null ? request.getPurchasePrice() : book.getPrice();

        return createCopies(book, request.getQuantity(), location, condition, price, source)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BookCopy> createCopies(
            Book book,
            int quantity,
            Location location,
            CopyCondition condition,
            BigDecimal purchasePrice,
            AcquisitionSource acquisitionSource
    ) {
        if (quantity <= 0) {
            return List.of();
        }
        int nextCopyNo = bookCopyRepository.findMaxCopyNo(book.getId()) + 1;
        Long userId = SecurityUtils.currentUserIdOrNull();
        Instant now = Instant.now();
        List<BookCopy> created = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            int copyNo = nextCopyNo + i;
            BookCopy copy = new BookCopy();
            copy.setBook(book);
            copy.setCopyNo(copyNo);
            copy.setBarcode(formatBarcode(book.getId(), copyNo));
            copy.setLocation(location);
            copy.setStatus(CopyStatus.AVAILABLE);
            copy.setCondition(condition != null ? condition : CopyCondition.GOOD);
            copy.setPurchasePrice(purchasePrice);
            copy.setAcquiredAt(now);
            copy.setAcquisitionSource(acquisitionSource);
            copy.setCreatedBy(userId);
            copy.setUpdatedBy(userId);
            created.add(bookCopyRepository.save(copy));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public BookCopyResponse lookupByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "Barcode không được để trống");
        }
        BookCopy copy = bookCopyRepository.findByBarcodeAndDeletedAtIsNull(barcode.trim())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bản sao", HttpStatus.NOT_FOUND));
        return toResponse(copy);
    }

    public BookCopyResponse update(Long id, UpdateCopyRequest request) {
        BookCopy copy = bookCopyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy bản sao", HttpStatus.NOT_FOUND));

        if (request.getStatus() != null) {
            copy.setStatus(request.getStatus());
        }
        if (request.getCondition() != null) {
            copy.setCondition(request.getCondition());
        }
        if (request.getNote() != null) {
            copy.setNote(request.getNote());
        }
        if (request.getLocationId() != null) {
            copy.setLocation(resolveLocation(request.getLocationId()));
        }
        copy.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toResponse(bookCopyRepository.save(copy));
    }

    @Transactional(readOnly = true)
    public PagedResult<BookCopyResponse> list(String q, CopyStatus status, int page, int size) {
        Pageable pageable = CatalogPageSupport.pageable(page, size, "copyNo,asc", "copyNo");
        Page<BookCopy> result = bookCopyRepository.findAll(
                BookCopySpecifications.withFilters(q, status),
                pageable
        );
        List<BookCopyResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedResult<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public static String formatBarcode(Long bookId, int copyNo) {
        return String.format("BC-%d-%03d", bookId, copyNo);
    }

    private Book requireActiveBook(Long bookId) {
        return bookRepository.findByIdAndDeletedAtIsNull(bookId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy sách", HttpStatus.NOT_FOUND));
    }

    private Location resolveLocation(Long locationId) {
        if (locationId == null) {
            return null;
        }
        return locationRepository.findByIdAndDeletedAtIsNull(locationId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy vị trí kệ", HttpStatus.NOT_FOUND));
    }

    private BookCopyResponse toResponse(BookCopy copy) {
        Book book = copy.getBook();
        Location location = copy.getLocation();
        return BookCopyResponse.builder()
                .id(copy.getId())
                .bookId(book != null ? book.getId() : null)
                .bookTitle(book != null ? book.getTitle() : null)
                .barcode(copy.getBarcode())
                .copyNo(copy.getCopyNo())
                .locationId(location != null ? location.getId() : null)
                .locationCode(location != null ? location.getCode() : null)
                .status(copy.getStatus())
                .condition(copy.getCondition())
                .purchasePrice(copy.getPurchasePrice())
                .acquiredAt(copy.getAcquiredAt())
                .acquisitionSource(copy.getAcquisitionSource())
                .note(copy.getNote())
                .createdAt(copy.getCreatedAt())
                .updatedAt(copy.getUpdatedAt())
                .build();
    }
}
