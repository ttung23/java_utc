package com.library.catalog.repository;

import com.library.catalog.entity.Book;
import com.library.common.enums.BookStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByIdAndDeletedAtIsNull(Long id);

    Optional<Book> findByIsbn13AndDeletedAtIsNull(String isbn13);

    List<Book> findByDeletedAtIsNullAndStatusOrderByCreatedAtDesc(BookStatus status, Pageable pageable);

    List<Book> findByDeletedAtIsNullAndStatusOrderByBorrowCountDesc(BookStatus status, Pageable pageable);

    long countByPublisher_Id(Long publisherId);

    long countByLanguage_Id(Long languageId);

    long countByDocumentType_Id(Long documentTypeId);
}
