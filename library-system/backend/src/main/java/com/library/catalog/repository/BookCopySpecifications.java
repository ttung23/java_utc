package com.library.catalog.repository;

import com.library.catalog.entity.BookCopy;
import com.library.common.enums.CopyStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class BookCopySpecifications {

    private BookCopySpecifications() {
    }

    public static Specification<BookCopy> withFilters(String q, CopyStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                var bookJoin = root.join("book", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("barcode")), pattern),
                        cb.like(cb.lower(bookJoin.get("title")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
