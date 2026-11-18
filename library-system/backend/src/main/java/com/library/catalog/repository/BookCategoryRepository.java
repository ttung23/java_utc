package com.library.catalog.repository;

import com.library.catalog.entity.BookCategory;
import com.library.catalog.entity.BookCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookCategoryRepository extends JpaRepository<BookCategory, BookCategoryId> {

    long countByCategoryId(Long categoryId);

    List<BookCategory> findByBookId(Long bookId);

    void deleteByBookId(Long bookId);
}
