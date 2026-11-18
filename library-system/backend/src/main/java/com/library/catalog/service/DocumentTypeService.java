package com.library.catalog.service;

import com.library.catalog.dto.DocumentTypeRequest;
import com.library.catalog.dto.DocumentTypeResponse;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.entity.DocumentType;
import com.library.catalog.repository.BookRepository;
import com.library.catalog.repository.DocumentTypeRepository;
import com.library.catalog.support.CatalogPageSupport;
import com.library.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentTypeService {

    private final DocumentTypeRepository documentTypeRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public PagedResult<DocumentTypeResponse> list(String q, int page, int size, String sort) {
        Pageable pageable = CatalogPageSupport.pageable(page, size, sort, "name");
        Page<DocumentType> result;
        if (StringUtils.hasText(q)) {
            String term = q.trim();
            result = documentTypeRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(term, term, pageable);
        } else {
            result = documentTypeRepository.findAll(pageable);
        }
        List<DocumentTypeResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedResult<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public DocumentTypeResponse getById(Long id) {
        return toResponse(require(id));
    }

    public DocumentTypeResponse create(DocumentTypeRequest request) {
        validateUniqueCode(request.getCode(), null);
        DocumentType type = new DocumentType();
        apply(type, request);
        return toResponse(documentTypeRepository.save(type));
    }

    public DocumentTypeResponse update(Long id, DocumentTypeRequest request) {
        DocumentType type = require(id);
        validateUniqueCode(request.getCode(), id);
        apply(type, request);
        return toResponse(documentTypeRepository.save(type));
    }

    public void delete(Long id) {
        DocumentType type = require(id);
        if (bookRepository.countByDocumentType_Id(id) > 0) {
            throw new BusinessException("DOCUMENT_TYPE_IN_USE", "Loại tài liệu đang được sử dụng bởi sách, không thể xóa");
        }
        documentTypeRepository.delete(type);
    }

    private void apply(DocumentType type, DocumentTypeRequest request) {
        type.setCode(request.getCode().trim());
        type.setName(request.getName().trim());
    }

    private void validateUniqueCode(String code, Long excludeId) {
        String trimmed = code.trim();
        boolean exists = excludeId == null
                ? documentTypeRepository.existsByCode(trimmed)
                : documentTypeRepository.existsByCodeAndIdNot(trimmed, excludeId);
        if (exists) {
            throw new BusinessException("DOCUMENT_TYPE_CODE_EXISTS", "Mã loại tài liệu đã tồn tại");
        }
    }

    private DocumentType require(Long id) {
        return documentTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy loại tài liệu", HttpStatus.NOT_FOUND));
    }

    private DocumentTypeResponse toResponse(DocumentType type) {
        return DocumentTypeResponse.builder()
                .id(type.getId())
                .code(type.getCode())
                .name(type.getName())
                .createdAt(type.getCreatedAt())
                .updatedAt(type.getUpdatedAt())
                .build();
    }
}
