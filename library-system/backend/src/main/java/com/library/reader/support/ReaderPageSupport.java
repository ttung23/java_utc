package com.library.reader.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

public final class ReaderPageSupport {

    private ReaderPageSupport() {
    }

    public static Pageable pageable(int page, int size, String sort, String defaultSortProperty) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 20 : Math.min(size, 100);
        return PageRequest.of(safePage, safeSize, parseSort(sort, defaultSortProperty));
    }

    public static Sort parseSort(String sort, String defaultSortProperty) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, defaultSortProperty);
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        if (!StringUtils.hasText(property)) {
            property = defaultSortProperty;
        }
        // Map FE-friendly aliases to entity paths used with join fetch aliases in Specs
        property = switch (property) {
            case "fullName", "email", "phone", "userStatus" -> "user." + ("userStatus".equals(property) ? "status" : property);
            case "readerTypeCode", "readerTypeName" -> "readerType." + ("readerTypeCode".equals(property) ? "code" : "name");
            case "cardStatus", "cardNumber", "cardExpiresAt" -> "id"; // card fields are derived; fallback id
            default -> property;
        };
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
            direction = Sort.Direction.DESC;
        }
        return Sort.by(direction, property);
    }
}
