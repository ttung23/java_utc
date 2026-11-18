package com.library.catalog.service;

import com.library.catalog.dto.CategoryRequest;
import com.library.catalog.dto.CategoryResponse;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.entity.Category;
import com.library.catalog.repository.BookCategoryRepository;
import com.library.catalog.repository.CategoryRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookCategoryRepository bookCategoryRepository;

    @Transactional(readOnly = true)
    public PagedResult<CategoryResponse> list(String q, int page, int size, String sort) {
        Pageable pageable = CatalogPageSupport.pageable(page, size, sort, "sortOrder");
        Page<Category> result;
        if (StringUtils.hasText(q)) {
            result = categoryRepository.findByDeletedAtIsNullAndNameContainingIgnoreCase(q.trim(), pageable);
        } else {
            result = categoryRepository.findByDeletedAtIsNull(pageable);
        }
        List<CategoryResponse> items = result.getContent().stream()
                .map(c -> toResponse(c, false))
                .toList();
        return new PagedResult<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> tree() {
        List<Category> all = categoryRepository.findByDeletedAtIsNullAndActiveTrueOrderBySortOrderAscNameAsc();
        Map<Long, CategoryResponse> nodes = new HashMap<>();
        for (Category category : all) {
            nodes.put(category.getId(), toResponse(category, true));
        }
        List<CategoryResponse> roots = new ArrayList<>();
        for (Category category : all) {
            CategoryResponse node = nodes.get(category.getId());
            Long parentId = category.getParent() != null ? category.getParent().getId() : null;
            if (parentId != null && nodes.containsKey(parentId)) {
                CategoryResponse parent = nodes.get(parentId);
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return toResponse(requireActive(id), false);
    }

    public CategoryResponse create(CategoryRequest request) {
        validateUniqueCode(request.getCode(), null);
        Category category = new Category();
        apply(category, request);
        category.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        category.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toResponse(categoryRepository.save(category), false);
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = requireActive(id);
        validateUniqueCode(request.getCode(), id);
        if (request.getParentId() != null && Objects.equals(request.getParentId(), id)) {
            throw new BusinessException("INVALID_PARENT", "Thể loại không thể là cha của chính nó");
        }
        apply(category, request);
        category.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toResponse(categoryRepository.save(category), false);
    }

    public void delete(Long id) {
        Category category = requireActive(id);
        if (categoryRepository.existsByParent_IdAndDeletedAtIsNull(id)) {
            throw new BusinessException("CATEGORY_HAS_CHILDREN", "Không thể xóa thể loại còn thể loại con");
        }
        if (bookCategoryRepository.countByCategoryId(id) > 0) {
            throw new BusinessException("CATEGORY_IN_USE", "Thể loại đang được gán cho sách, không thể xóa");
        }
        category.setDeletedAt(Instant.now());
        category.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        categoryRepository.save(category);
    }

    private void apply(Category category, CategoryRequest request) {
        category.setCode(StringUtils.hasText(request.getCode()) ? request.getCode().trim() : null);
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setActive(request.getActive() == null || request.getActive());
        if (request.getParentId() != null) {
            Category parent = requireActive(request.getParentId());
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
    }

    private void validateUniqueCode(String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        String trimmed = code.trim();
        boolean exists = excludeId == null
                ? categoryRepository.existsByCodeAndDeletedAtIsNull(trimmed)
                : categoryRepository.existsByCodeAndIdNotAndDeletedAtIsNull(trimmed, excludeId);
        if (exists) {
            throw new BusinessException("CATEGORY_CODE_EXISTS", "Mã thể loại đã tồn tại");
        }
    }

    private Category requireActive(Long id) {
        return categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy thể loại", HttpStatus.NOT_FOUND));
    }

    private CategoryResponse toResponse(Category category, boolean withEmptyChildren) {
        Category parent = category.getParent();
        return CategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(parent != null ? parent.getId() : null)
                .parentName(parent != null ? parent.getName() : null)
                .sortOrder(category.getSortOrder())
                .active(category.isActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .children(withEmptyChildren ? new ArrayList<>() : null)
                .build();
    }
}
