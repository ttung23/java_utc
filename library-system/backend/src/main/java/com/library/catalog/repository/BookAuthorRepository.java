package com.library.catalog.repository;

import com.library.catalog.entity.BookAuthor;
import com.library.catalog.entity.BookAuthorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookAuthorRepository extends JpaRepository<BookAuthor, BookAuthorId> {

    long countByAuthorId(Long authorId);

    List<BookAuthor> findByBookIdOrderBySortOrderAsc(Long bookId);

    void deleteByBookId(Long bookId);
}
