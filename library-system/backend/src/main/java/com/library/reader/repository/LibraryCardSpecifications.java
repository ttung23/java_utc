package com.library.reader.repository;

import com.library.common.enums.CardStatus;
import com.library.reader.entity.LibraryCard;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class LibraryCardSpecifications {

    private LibraryCardSpecifications() {
    }

    public static Specification<LibraryCard> withFilters(Long readerId, CardStatus status, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            root.join("reader", JoinType.INNER).join("user", JoinType.INNER);

            if (query != null && query.getResultType() != null && query.getResultType() != Long.class) {
                query.distinct(true);
            }

            if (readerId != null) {
                predicates.add(cb.equal(root.get("reader").get("id"), readerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("cardNumber")), pattern),
                        cb.like(cb.lower(root.get("barcode")), pattern),
                        cb.like(cb.lower(root.get("reader").get("readerCode")), pattern),
                        cb.like(cb.lower(root.get("reader").get("user").get("fullName")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
