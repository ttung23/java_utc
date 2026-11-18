package com.library.catalog.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

public final class CatalogPageSupport {

    private CatalogPageSupport() {
    }

    public static int clampSize(int size) {
        if (size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }

    public static int normalizePage(int page) {
        return Math.max(page, 0);
    }

    public static Pageable pageable(int page, int size, String sort, String defaultSortProperty) {
        int safePage = normalizePage(page);
        int safeSize = clampSize(size);
        Sort sortObj = parseSort(sort, defaultSortProperty);
        return PageRequest.of(safePage, safeSize, sortObj);
    }

    public static Sort parseSort(String sort, String defaultSortProperty) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.ASC, defaultSortProperty);
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        if (!StringUtils.hasText(property)) {
            property = defaultSortProperty;
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
            direction = Sort.Direction.DESC;
        }
        return Sort.by(direction, property);
    }
}
