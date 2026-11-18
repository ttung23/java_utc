package com.library.catalog.repository;

import com.library.catalog.entity.Book;
import com.library.catalog.entity.BookAuthor;
import com.library.catalog.entity.BookCategory;
import com.library.catalog.entity.BookCopy;
import com.library.common.enums.BookStatus;
import com.library.common.enums.CopyStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class BookSpecifications {

    private BookSpecifications() {
    }

    public static Specification<Book> withFilters(
            String q,
            Long categoryId,
            Long authorId,
            Long publisherId,
            Integer year,
            Long languageId,
            Boolean available,
            BookStatus status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("isbn13"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("isbn10"), "")), pattern)
                ));
            }

            if (publisherId != null) {
                predicates.add(cb.equal(root.get("publisher").get("id"), publisherId));
            }

            if (year != null) {
                predicates.add(cb.equal(root.get("publishYear"), year));
            }

            if (languageId != null) {
                predicates.add(cb.equal(root.get("language").get("id"), languageId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (categoryId != null) {
                Subquery<Long> categorySub = query.subquery(Long.class);
                Root<BookCategory> bcRoot = categorySub.from(BookCategory.class);
                categorySub.select(bcRoot.get("bookId"));
                categorySub.where(
                        cb.equal(bcRoot.get("bookId"), root.get("id")),
                        cb.equal(bcRoot.get("categoryId"), categoryId)
                );
                predicates.add(cb.exists(categorySub));
            }

            if (authorId != null) {
                Subquery<Long> authorSub = query.subquery(Long.class);
                Root<BookAuthor> baRoot = authorSub.from(BookAuthor.class);
                authorSub.select(baRoot.get("bookId"));
                authorSub.where(
                        cb.equal(baRoot.get("bookId"), root.get("id")),
                        cb.equal(baRoot.get("authorId"), authorId)
                );
                predicates.add(cb.exists(authorSub));
            }

            if (Boolean.TRUE.equals(available)) {
                Subquery<Long> availableSub = query.subquery(Long.class);
                Root<BookCopy> copyRoot = availableSub.from(BookCopy.class);
                availableSub.select(copyRoot.get("id"));
                availableSub.where(
                        cb.equal(copyRoot.get("book").get("id"), root.get("id")),
                        cb.equal(copyRoot.get("status"), CopyStatus.AVAILABLE),
                        cb.isNull(copyRoot.get("deletedAt"))
                );
                predicates.add(cb.exists(availableSub));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
