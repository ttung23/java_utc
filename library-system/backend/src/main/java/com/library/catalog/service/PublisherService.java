package com.library.catalog.service;

import com.library.catalog.dto.PagedResult;
import com.library.catalog.dto.PublisherRequest;
import com.library.catalog.dto.PublisherResponse;
import com.library.catalog.entity.Publisher;
import com.library.catalog.repository.BookRepository;
import com.library.catalog.repository.PublisherRepository;
import com.library.catalog.support.CatalogPageSupport;
import com.library.common.exception.BusinessException;
import com.library.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PublisherService {

    private final PublisherRepository publisherRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public PagedResult<PublisherResponse> list(String q, int page, int size, String sort) {
        Pageable pageable = CatalogPageSupport.pageable(page, size, sort, "name");
        Page<Publisher> result;
        if (StringUtils.hasText(q)) {
            result = publisherRepository.findByDeletedAtIsNullAndNameContainingIgnoreCase(q.trim(), pageable);
        } else {
            result = publisherRepository.findByDeletedAtIsNull(pageable);
        }
        List<PublisherResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedResult<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PublisherResponse getById(Long id) {
        return toResponse(requireActive(id));
    }

    public PublisherResponse create(PublisherRequest request) {
        Publisher publisher = new Publisher();
        apply(publisher, request);
        publisher.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        publisher.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toResponse(publisherRepository.save(publisher));
    }

    public PublisherResponse update(Long id, PublisherRequest request) {
        Publisher publisher = requireActive(id);
        apply(publisher, request);
        publisher.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toResponse(publisherRepository.save(publisher));
    }

    public void delete(Long id) {
        Publisher publisher = requireActive(id);
        if (bookRepository.countByPublisher_Id(id) > 0) {
            throw new BusinessException("PUBLISHER_IN_USE", "Nhà xuất bản đang được sử dụng bởi sách, không thể xóa");
        }
        publisher.setDeletedAt(Instant.now());
        publisher.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        publisherRepository.save(publisher);
    }

    private void apply(Publisher publisher, PublisherRequest request) {
        publisher.setName(request.getName().trim());
        publisher.setAddress(request.getAddress());
        publisher.setPhone(request.getPhone());
        publisher.setEmail(request.getEmail());
        publisher.setWebsite(request.getWebsite());
    }

    private Publisher requireActive(Long id) {
        return publisherRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy nhà xuất bản", HttpStatus.NOT_FOUND));
    }

    private PublisherResponse toResponse(Publisher publisher) {
        return PublisherResponse.builder()
                .id(publisher.getId())
                .name(publisher.getName())
                .address(publisher.getAddress())
                .phone(publisher.getPhone())
                .email(publisher.getEmail())
                .website(publisher.getWebsite())
                .createdAt(publisher.getCreatedAt())
                .updatedAt(publisher.getUpdatedAt())
                .build();
    }
}
