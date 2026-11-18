package com.library.reader.repository;

import com.library.common.enums.CardStatus;
import com.library.reader.entity.LibraryCard;
import com.library.reader.entity.Reader;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ReaderSpecifications {

    private ReaderSpecifications() {
    }

    public static Specification<Reader> withFilters(
            String q,
            Long readerTypeId,
            CardStatus cardStatus,
            Boolean blacklisted
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            Join<?, ?> userJoin = root.join("user", JoinType.INNER);
            root.join("readerType", JoinType.INNER);

            if (query != null && query.getResultType() != null && query.getResultType() != Long.class) {
                query.distinct(true);
            }

            if (readerTypeId != null) {
                predicates.add(cb.equal(root.get("readerType").get("id"), readerTypeId));
            }

            if (blacklisted != null) {
                predicates.add(cb.equal(root.get("blacklisted"), blacklisted));
            }

            if (cardStatus != null) {
                Subquery<Long> cardSub = query.subquery(Long.class);
                Root<LibraryCard> cardRoot = cardSub.from(LibraryCard.class);
                cardSub.select(cardRoot.get("id"));
                cardSub.where(
                        cb.equal(cardRoot.get("reader").get("id"), root.get("id")),
                        cb.equal(cardRoot.get("status"), cardStatus),
                        cb.isNull(cardRoot.get("deletedAt"))
                );
                predicates.add(cb.exists(cardSub));
            }

            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                Subquery<Long> cardQ = query.subquery(Long.class);
                Root<LibraryCard> cardRoot = cardQ.from(LibraryCard.class);
                cardQ.select(cardRoot.get("id"));
                cardQ.where(
                        cb.equal(cardRoot.get("reader").get("id"), root.get("id")),
                        cb.isNull(cardRoot.get("deletedAt")),
                        cb.or(
                                cb.like(cb.lower(cardRoot.get("cardNumber")), pattern),
                                cb.like(cb.lower(cardRoot.get("barcode")), pattern)
                        )
                );
                predicates.add(cb.or(
                        cb.like(cb.lower(userJoin.get("fullName")), pattern),
                        cb.like(cb.lower(userJoin.get("email")), pattern),
                        cb.like(cb.lower(cb.coalesce(userJoin.get("phone"), "")), pattern),
                        cb.like(cb.lower(root.get("readerCode")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("idNumber"), "")), pattern),
                        cb.exists(cardQ)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
