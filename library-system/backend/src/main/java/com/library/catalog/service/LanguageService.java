package com.library.catalog.service;

import com.library.catalog.dto.LanguageRequest;
import com.library.catalog.dto.LanguageResponse;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.entity.Language;
import com.library.catalog.repository.BookRepository;
import com.library.catalog.repository.LanguageRepository;
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
public class LanguageService {

    private final LanguageRepository languageRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public PagedResult<LanguageResponse> list(String q, int page, int size, String sort) {
        Pageable pageable = CatalogPageSupport.pageable(page, size, sort, "name");
        Page<Language> result;
        if (StringUtils.hasText(q)) {
            String term = q.trim();
            result = languageRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(term, term, pageable);
        } else {
            result = languageRepository.findAll(pageable);
        }
        List<LanguageResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedResult<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public LanguageResponse getById(Long id) {
        return toResponse(require(id));
    }

    public LanguageResponse create(LanguageRequest request) {
        validateUniqueCode(request.getCode(), null);
        Language language = new Language();
        apply(language, request);
        return toResponse(languageRepository.save(language));
    }

    public LanguageResponse update(Long id, LanguageRequest request) {
        Language language = require(id);
        validateUniqueCode(request.getCode(), id);
        apply(language, request);
        return toResponse(languageRepository.save(language));
    }

    public void delete(Long id) {
        Language language = require(id);
        if (bookRepository.countByLanguage_Id(id) > 0) {
            throw new BusinessException("LANGUAGE_IN_USE", "Ngôn ngữ đang được sử dụng bởi sách, không thể xóa");
        }
        languageRepository.delete(language);
    }

    private void apply(Language language, LanguageRequest request) {
        language.setCode(request.getCode().trim());
        language.setName(request.getName().trim());
    }

    private void validateUniqueCode(String code, Long excludeId) {
        String trimmed = code.trim();
        boolean exists = excludeId == null
                ? languageRepository.existsByCode(trimmed)
                : languageRepository.existsByCodeAndIdNot(trimmed, excludeId);
        if (exists) {
            throw new BusinessException("LANGUAGE_CODE_EXISTS", "Mã ngôn ngữ đã tồn tại");
        }
    }

    private Language require(Long id) {
        return languageRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy ngôn ngữ", HttpStatus.NOT_FOUND));
    }

    private LanguageResponse toResponse(Language language) {
        return LanguageResponse.builder()
                .id(language.getId())
                .code(language.getCode())
                .name(language.getName())
                .createdAt(language.getCreatedAt())
                .updatedAt(language.getUpdatedAt())
                .build();
    }
}
