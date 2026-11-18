package com.library.catalog.entity;

import com.library.common.enums.BookStatus;
import com.library.common.persistence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "books")
public class Book extends SoftDeletableEntity {

    @Column(length = 13)
    private String isbn13;

    @Column(length = 10)
    private String isbn10;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "original_title", length = 500)
    private String originalTitle;

    @Column(length = 500)
    private String subtitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @Column(name = "publish_year")
    private Integer publishYear;

    @Column(length = 100)
    private String edition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id")
    private Language language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id")
    private DocumentType documentType;

    private Integer pages;

    @Column(length = 50)
    private String size;

    @Column(precision = 12, scale = 0)
    private BigDecimal price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "classification_code", length = 100)
    private String classificationCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> tags;

    @Column(name = "is_reference", nullable = false)
    private boolean reference = false;

    @Column(nullable = false)
    private boolean loanable = true;

    @Column(name = "loan_days_override")
    private Integer loanDaysOverride;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookStatus status = BookStatus.ACTIVE;

    @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount = 0;

    @Column(name = "borrow_count", nullable = false)
    private int borrowCount = 0;

    @Column(name = "search_vector", insertable = false, updatable = false, columnDefinition = "tsvector")
    private String searchVector;
}
