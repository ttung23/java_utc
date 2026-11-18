package com.library.catalog.service;

import com.library.catalog.dto.AuthorRequest;
import com.library.catalog.dto.AuthorResponse;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.entity.Author;
import com.library.catalog.repository.AuthorRepository;
import com.library.catalog.repository.BookAuthorRepository;
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
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookAuthorRepository bookAuthorRepository;

    @Transactional(readOnly = true)
    public PagedResult<AuthorResponse> list(String q, int page, int size, String sort) {
        Pageable pageable = CatalogPageSupport.pageable(page, size, sort, "name");
        Page<Author> result;
        if (StringUtils.hasText(q)) {
            result = authorRepository.findByDeletedAtIsNullAndNameContainingIgnoreCase(q.trim(), pageable);
        } else {
            result = authorRepository.findByDeletedAtIsNull(pageable);
        }
        List<AuthorResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedResult<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AuthorResponse getById(Long id) {
        return toResponse(requireActive(id));
    }

    public AuthorResponse create(AuthorRequest request) {
        Author author = new Author();
        apply(author, request);
        author.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        author.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toResponse(authorRepository.save(author));
    }

    public AuthorResponse update(Long id, AuthorRequest request) {
        Author author = requireActive(id);
        apply(author, request);
        author.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toResponse(authorRepository.save(author));
    }

    public void delete(Long id) {
        Author author = requireActive(id);
        if (bookAuthorRepository.countByAuthorId(id) > 0) {
            throw new BusinessException("AUTHOR_IN_USE", "Tác giả đang được gán cho sách, không thể xóa");
        }
        author.setDeletedAt(Instant.now());
        author.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        authorRepository.save(author);
    }

    private void apply(Author author, AuthorRequest request) {
        author.setName(request.getName().trim());
        author.setPenName(request.getPenName());
        author.setNationality(request.getNationality());
        author.setBirthYear(request.getBirthYear());
        author.setDeathYear(request.getDeathYear());
        author.setBio(request.getBio());
        author.setPhotoUrl(request.getPhotoUrl());
    }

    private Author requireActive(Long id) {
        return authorRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy tác giả", HttpStatus.NOT_FOUND));
    }

    private AuthorResponse toResponse(Author author) {
        return AuthorResponse.builder()
                .id(author.getId())
                .name(author.getName())
                .penName(author.getPenName())
                .nationality(author.getNationality())
                .birthYear(author.getBirthYear())
                .deathYear(author.getDeathYear())
                .bio(author.getBio())
                .photoUrl(author.getPhotoUrl())
                .createdAt(author.getCreatedAt())
                .updatedAt(author.getUpdatedAt())
                .build();
    }
}
