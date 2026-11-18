package com.library.catalog.service;

import com.library.catalog.dto.BookDetailResponse;
import com.library.catalog.dto.BookListItem;
import com.library.catalog.dto.BookRequest;
import com.library.catalog.dto.CopySummary;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.entity.Author;
import com.library.catalog.entity.Book;
import com.library.catalog.entity.BookAuthor;
import com.library.catalog.entity.BookCategory;
import com.library.catalog.entity.Category;
import com.library.catalog.entity.DocumentType;
import com.library.catalog.entity.Language;
import com.library.catalog.entity.Location;
import com.library.catalog.entity.Publisher;
import com.library.catalog.repository.AuthorRepository;
import com.library.catalog.repository.BookAuthorRepository;
import com.library.catalog.repository.BookCategoryRepository;
import com.library.catalog.repository.BookCopyRepository;
import com.library.catalog.repository.BookRepository;
import com.library.catalog.repository.BookSpecifications;
import com.library.catalog.repository.CategoryRepository;
import com.library.catalog.repository.DocumentTypeRepository;
import com.library.catalog.repository.LanguageRepository;
import com.library.catalog.repository.LocationRepository;
import com.library.catalog.repository.PublisherRepository;
import com.library.catalog.support.CatalogPageSupport;
import com.library.common.enums.BookAuthorRole;
import com.library.common.enums.BookStatus;
import com.library.common.enums.CopyCondition;
import com.library.common.enums.CopyStatus;
import com.library.common.exception.BusinessException;
import com.library.common.storage.FileStorageService;
import com.library.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final LanguageRepository languageRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final LocationRepository locationRepository;
    private final BookCopyService bookCopyService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public PagedResult<BookListItem> list(
            String q,
            Long categoryId,
            Long authorId,
            Long publisherId,
            Integer year,
            Long languageId,
            Boolean available,
            BookStatus status,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = CatalogPageSupport.pageable(page, size, sort, "title");
        Page<Book> result = bookRepository.findAll(
                BookSpecifications.withFilters(q, categoryId, authorId, publisherId, year, languageId, available, status),
                pageable
        );
        List<BookListItem> items = toListItems(result.getContent());
        return new PagedResult<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public BookDetailResponse getById(Long id) {
        Book book = requireActive(id);
        return toDetail(book);
    }

    @Transactional(readOnly = true)
    public List<BookListItem> listNew() {
        List<Book> books = bookRepository.findByDeletedAtIsNullAndStatusOrderByCreatedAtDesc(
                BookStatus.ACTIVE,
                PageRequest.of(0, 12)
        );
        return toListItems(books);
    }

    @Transactional(readOnly = true)
    public List<BookListItem> listPopular() {
        List<Book> books = bookRepository.findByDeletedAtIsNullAndStatusOrderByBorrowCountDesc(
                BookStatus.ACTIVE,
                PageRequest.of(0, 12)
        );
        return toListItems(books);
    }

    public BookDetailResponse create(BookRequest request) {
        Book book = new Book();
        applyFields(book, request);
        book.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        book.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        book = bookRepository.save(book);

        replaceAuthors(book.getId(), request.getAuthorIds());
        replaceCategories(book.getId(), request.getCategoryIds());

        int initialCopies = request.getInitialCopies() != null ? request.getInitialCopies() : 0;
        if (initialCopies > 0) {
            Location location = resolveLocation(request.getLocationId());
            bookCopyService.createCopies(
                    book,
                    initialCopies,
                    location,
                    CopyCondition.GOOD,
                    book.getPrice(),
                    null
            );
        }

        return toDetail(book);
    }

    public BookDetailResponse update(Long id, BookRequest request) {
        Book book = requireActive(id);
        applyFields(book, request);
        book.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        book = bookRepository.save(book);

        replaceAuthors(book.getId(), request.getAuthorIds());
        replaceCategories(book.getId(), request.getCategoryIds());

        return toDetail(book);
    }

    public void delete(Long id) {
        Book book = requireActive(id);
        book.setDeletedAt(Instant.now());
        book.setStatus(BookStatus.HIDDEN);
        book.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        bookRepository.save(book);
    }

    public BookDetailResponse uploadCover(Long id, MultipartFile file) {
        Book book = requireActive(id);
        String url = fileStorageService.storeCover(file);
        book.setCoverUrl(url);
        book.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        bookRepository.save(book);
        return toDetail(book);
    }

    @Transactional(readOnly = true)
    public List<BookListItem> related(Long id) {
        Book book = requireActive(id);
        Set<Long> relatedIds = new LinkedHashSet<>();
        Set<Long> authorIds = new LinkedHashSet<>();
        bookAuthorRepository.findByBookIdOrderBySortOrderAsc(id).forEach(ba -> authorIds.add(ba.getAuthorId()));
        for (Long authorId : authorIds) {
            bookAuthorRepository.findAll().stream()
                    .filter(x -> Objects.equals(x.getAuthorId(), authorId))
                    .map(BookAuthor::getBookId)
                    .filter(bookId -> !Objects.equals(bookId, id))
                    .forEach(relatedIds::add);
        }
        Set<Long> categoryIds = new LinkedHashSet<>();
        bookCategoryRepository.findByBookId(id).forEach(bc -> categoryIds.add(bc.getCategoryId()));
        for (Long categoryId : categoryIds) {
            bookCategoryRepository.findAll().stream()
                    .filter(x -> Objects.equals(x.getCategoryId(), categoryId))
                    .map(BookCategory::getBookId)
                    .filter(bookId -> !Objects.equals(bookId, id))
                    .forEach(relatedIds::add);
        }
        List<Book> books = relatedIds.stream()
                .map(bookRepository::findByIdAndDeletedAtIsNull)
                .flatMap(Optional::stream)
                .filter(b -> b.getStatus() == BookStatus.ACTIVE)
                .limit(12)
                .toList();
        if (books.isEmpty() && book.getPublisher() != null) {
            Long publisherId = book.getPublisher().getId();
            books = bookRepository.findAll().stream()
                    .filter(b -> b.getDeletedAt() == null && b.getStatus() == BookStatus.ACTIVE)
                    .filter(b -> b.getPublisher() != null && Objects.equals(b.getPublisher().getId(), publisherId))
                    .filter(b -> !Objects.equals(b.getId(), id))
                    .limit(12)
                    .toList();
        }
        return toListItems(books);
    }

    private void applyFields(Book book, BookRequest request) {
        book.setTitle(request.getTitle().trim());
        book.setIsbn13(trimToNull(request.getIsbn13()));
        book.setIsbn10(trimToNull(request.getIsbn10()));
        book.setSubtitle(trimToNull(request.getSubtitle()));
        book.setPublisher(resolvePublisher(request.getPublisherId()));
        book.setPublishYear(request.getPublishYear());
        book.setLanguage(resolveLanguage(request.getLanguageId()));
        book.setDocumentType(resolveDocumentType(request.getDocumentTypeId()));
        book.setPages(request.getPages());
        book.setPrice(request.getPrice());
        book.setDescription(request.getDescription());
        book.setCoverUrl(trimToNull(request.getCoverUrl()));
        book.setClassificationCode(trimToNull(request.getClassificationCode()));
        book.setTags(request.getTags());
        if (request.getReference() != null) {
            book.setReference(request.getReference());
        }
        if (request.getLoanable() != null) {
            book.setLoanable(request.getLoanable());
        }
        book.setLoanDaysOverride(request.getLoanDaysOverride());
        if (request.getStatus() != null) {
            book.setStatus(request.getStatus());
        } else if (book.getId() == null) {
            book.setStatus(BookStatus.ACTIVE);
        }
    }

    private void replaceAuthors(Long bookId, List<Long> authorIds) {
        bookAuthorRepository.deleteByBookId(bookId);
        if (authorIds == null || authorIds.isEmpty()) {
            return;
        }
        Set<Long> unique = new LinkedHashSet<>(authorIds.stream().filter(Objects::nonNull).toList());
        int order = 0;
        for (Long authorId : unique) {
            Author author = authorRepository.findByIdAndDeletedAtIsNull(authorId)
                    .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy tác giả id=" + authorId, HttpStatus.NOT_FOUND));
            BookAuthor link = new BookAuthor();
            link.setBookId(bookId);
            link.setAuthorId(author.getId());
            link.setRole(BookAuthorRole.AUTHOR);
            link.setSortOrder(order++);
            bookAuthorRepository.save(link);
        }
    }

    private void replaceCategories(Long bookId, List<Long> categoryIds) {
        bookCategoryRepository.deleteByBookId(bookId);
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        Set<Long> unique = new LinkedHashSet<>(categoryIds.stream().filter(Objects::nonNull).toList());
        for (Long categoryId : unique) {
            Category category = categoryRepository.findByIdAndDeletedAtIsNull(categoryId)
                    .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy thể loại id=" + categoryId, HttpStatus.NOT_FOUND));
            BookCategory link = new BookCategory();
            link.setBookId(bookId);
            link.setCategoryId(category.getId());
            bookCategoryRepository.save(link);
        }
    }

    private List<BookListItem> toListItems(List<Book> books) {
        if (books.isEmpty()) {
            return List.of();
        }
        List<Long> ids = books.stream().map(Book::getId).toList();
        Map<Long, Long> totalMap = toCountMap(bookCopyRepository.countTotalByBookIds(ids));
        Map<Long, Long> availableMap = toCountMap(
                bookCopyRepository.countByBookIdsAndStatus(ids, CopyStatus.AVAILABLE)
        );
        List<BookListItem> items = new ArrayList<>(books.size());
        for (Book book : books) {
            items.add(BookListItem.builder()
                    .id(book.getId())
                    .title(book.getTitle())
                    .isbn13(book.getIsbn13())
                    .publisherName(book.getPublisher() != null ? book.getPublisher().getName() : null)
                    .publishYear(book.getPublishYear())
                    .status(book.getStatus())
                    .coverUrl(book.getCoverUrl())
                    .totalCopies(totalMap.getOrDefault(book.getId(), 0L))
                    .availableCopies(availableMap.getOrDefault(book.getId(), 0L))
                    .avgRating(book.getAvgRating() != null ? book.getAvgRating() : BigDecimal.ZERO)
                    .build());
        }
        return items;
    }

    private BookDetailResponse toDetail(Book book) {
        List<BookAuthor> authors = bookAuthorRepository.findByBookIdOrderBySortOrderAsc(book.getId());
        List<Long> authorIds = new ArrayList<>();
        List<String> authorNames = new ArrayList<>();
        for (BookAuthor link : authors) {
            authorIds.add(link.getAuthorId());
            if (link.getAuthor() != null) {
                authorNames.add(link.getAuthor().getName());
            } else {
                authorRepository.findById(link.getAuthorId())
                        .ifPresent(a -> authorNames.add(a.getName()));
            }
        }

        List<BookCategory> categories = bookCategoryRepository.findByBookId(book.getId());
        List<Long> categoryIds = new ArrayList<>();
        List<String> categoryNames = new ArrayList<>();
        for (BookCategory link : categories) {
            categoryIds.add(link.getCategoryId());
            if (link.getCategory() != null) {
                categoryNames.add(link.getCategory().getName());
            } else {
                categoryRepository.findById(link.getCategoryId())
                        .ifPresent(c -> categoryNames.add(c.getName()));
            }
        }

        return BookDetailResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .isbn13(book.getIsbn13())
                .isbn10(book.getIsbn10())
                .subtitle(book.getSubtitle())
                .publisherId(book.getPublisher() != null ? book.getPublisher().getId() : null)
                .publisherName(book.getPublisher() != null ? book.getPublisher().getName() : null)
                .publishYear(book.getPublishYear())
                .languageId(book.getLanguage() != null ? book.getLanguage().getId() : null)
                .languageName(book.getLanguage() != null ? book.getLanguage().getName() : null)
                .documentTypeId(book.getDocumentType() != null ? book.getDocumentType().getId() : null)
                .documentTypeName(book.getDocumentType() != null ? book.getDocumentType().getName() : null)
                .pages(book.getPages())
                .price(book.getPrice())
                .description(book.getDescription())
                .coverUrl(book.getCoverUrl())
                .classificationCode(book.getClassificationCode())
                .tags(book.getTags())
                .reference(book.isReference())
                .loanable(book.isLoanable())
                .loanDaysOverride(book.getLoanDaysOverride())
                .status(book.getStatus())
                .avgRating(book.getAvgRating())
                .ratingCount(book.getRatingCount())
                .borrowCount(book.getBorrowCount())
                .authorIds(authorIds)
                .authorNames(authorNames)
                .categoryIds(categoryIds)
                .categoryNames(categoryNames)
                .copySummary(buildCopySummary(book.getId()))
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }

    private CopySummary buildCopySummary(Long bookId) {
        Map<CopyStatus, Long> byStatus = new HashMap<>();
        for (Object[] row : bookCopyRepository.countGroupByStatus(bookId)) {
            byStatus.put((CopyStatus) row[0], (Long) row[1]);
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return CopySummary.builder()
                .total(total)
                .available(byStatus.getOrDefault(CopyStatus.AVAILABLE, 0L))
                .borrowed(byStatus.getOrDefault(CopyStatus.BORROWED, 0L))
                .hold(byStatus.getOrDefault(CopyStatus.HOLD, 0L))
                .lost(byStatus.getOrDefault(CopyStatus.LOST, 0L))
                .build();
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private Book requireActive(Long id) {
        return bookRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy sách", HttpStatus.NOT_FOUND));
    }

    private Publisher resolvePublisher(Long id) {
        if (id == null) {
            return null;
        }
        return publisherRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy NXB", HttpStatus.NOT_FOUND));
    }

    private Language resolveLanguage(Long id) {
        if (id == null) {
            return null;
        }
        return languageRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy ngôn ngữ", HttpStatus.NOT_FOUND));
    }

    private DocumentType resolveDocumentType(Long id) {
        if (id == null) {
            return null;
        }
        return documentTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy loại tài liệu", HttpStatus.NOT_FOUND));
    }

    private Location resolveLocation(Long id) {
        if (id == null) {
            return null;
        }
        return locationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy vị trí kệ", HttpStatus.NOT_FOUND));
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
