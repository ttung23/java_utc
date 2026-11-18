package com.library.cms.service;

import com.library.borrow.repository.BorrowSlipItemRepository;
import com.library.catalog.entity.Book;
import com.library.catalog.repository.BookRepository;
import com.library.cms.entity.Banner;
import com.library.cms.entity.Favorite;
import com.library.cms.entity.News;
import com.library.cms.entity.PurchaseSuggestion;
import com.library.cms.entity.Review;
import com.library.cms.repository.BannerRepository;
import com.library.cms.repository.FavoriteRepository;
import com.library.cms.repository.NewsRepository;
import com.library.cms.repository.PurchaseSuggestionRepository;
import com.library.cms.repository.ReviewRepository;
import com.library.common.enums.NewsStatus;
import com.library.common.enums.ReviewStatus;
import com.library.common.enums.SuggestionStatus;
import com.library.common.enums.UserRole;
import com.library.common.exception.BusinessException;
import com.library.security.AuthUserPrincipal;
import com.library.security.SecurityUtils;
import com.library.user.entity.User;
import com.library.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CmsService {

    private final NewsRepository newsRepository;
    private final BannerRepository bannerRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;
    private final PurchaseSuggestionRepository suggestionRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowSlipItemRepository borrowSlipItemRepository;

    // ---- News ----
    public Map<String, Object> createNews(String title, String content, String coverUrl, Boolean pinned, NewsStatus status) {
        News news = new News();
        news.setTitle(title);
        news.setSlug(uniqueSlug(title));
        news.setContent(content);
        news.setCoverUrl(coverUrl);
        news.setPinned(Boolean.TRUE.equals(pinned));
        news.setStatus(status != null ? status : NewsStatus.DRAFT);
        if (news.getStatus() == NewsStatus.PUBLISHED) {
            news.setPublishedAt(Instant.now());
        }
        news.setAuthor(userRepository.findById(SecurityUtils.requireCurrentUser().getId()).orElse(null));
        news.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        newsRepository.save(news);
        return newsMap(news);
    }

    public Map<String, Object> updateNews(Long id, String title, String content, String coverUrl, Boolean pinned, NewsStatus status) {
        News news = newsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy tin", HttpStatus.NOT_FOUND));
        if (title != null) {
            news.setTitle(title);
        }
        if (content != null) {
            news.setContent(content);
        }
        if (coverUrl != null) {
            news.setCoverUrl(coverUrl);
        }
        if (pinned != null) {
            news.setPinned(pinned);
        }
        if (status != null) {
            news.setStatus(status);
            if (status == NewsStatus.PUBLISHED && news.getPublishedAt() == null) {
                news.setPublishedAt(Instant.now());
            }
        }
        news.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        newsRepository.save(news);
        return newsMap(news);
    }

    public void deleteNews(Long id) {
        News news = newsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy tin", HttpStatus.NOT_FOUND));
        news.setDeletedAt(Instant.now());
        newsRepository.save(news);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> publicNews() {
        return newsRepository.findByStatusAndDeletedAtIsNullOrderByPinnedDescPublishedAtDesc(NewsStatus.PUBLISHED)
                .stream().map(this::newsMap).toList();
    }

    @Transactional(readOnly = true)
    public PageResult listNews(int page, int size) {
        Page<News> result = newsRepository.findByDeletedAtIsNull(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult(result.map(this::newsMap).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> newsBySlug(String slug) {
        return newsMap(newsRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy tin", HttpStatus.NOT_FOUND)));
    }

    // ---- Banners ----
    public Map<String, Object> createBanner(String imageUrl, String link, Integer sortOrder, Boolean active) {
        Banner b = new Banner();
        b.setImageUrl(imageUrl);
        b.setLink(link);
        b.setSortOrder(sortOrder != null ? sortOrder : 0);
        b.setActive(active == null || active);
        b.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        bannerRepository.save(b);
        return bannerMap(b);
    }

    public Map<String, Object> updateBanner(Long id, String imageUrl, String link, Integer sortOrder, Boolean active) {
        Banner b = bannerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy banner", HttpStatus.NOT_FOUND));
        if (imageUrl != null) b.setImageUrl(imageUrl);
        if (link != null) b.setLink(link);
        if (sortOrder != null) b.setSortOrder(sortOrder);
        if (active != null) b.setActive(active);
        bannerRepository.save(b);
        return bannerMap(b);
    }

    public void deleteBanner(Long id) {
        Banner b = bannerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy banner", HttpStatus.NOT_FOUND));
        b.setDeletedAt(Instant.now());
        bannerRepository.save(b);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> publicBanners() {
        return bannerRepository.findByDeletedAtIsNullAndActiveTrueOrderBySortOrderAsc().stream().map(this::bannerMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listBanners() {
        return bannerRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream().map(this::bannerMap).toList();
    }

    // ---- Favorites ----
    public void addFavorite(Long bookId) {
        Long userId = SecurityUtils.requireCurrentUser().getId();
        requireBook(bookId);
        if (!favoriteRepository.existsByUserIdAndBookId(userId, bookId)) {
            Favorite f = new Favorite();
            f.setUserId(userId);
            f.setBookId(bookId);
            favoriteRepository.save(f);
        }
    }

    public void removeFavorite(Long bookId) {
        favoriteRepository.deleteByUserIdAndBookId(SecurityUtils.requireCurrentUser().getId(), bookId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myFavorites() {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(SecurityUtils.requireCurrentUser().getId())
                .stream().map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("bookId", f.getBookId());
                    String title = bookRepository.findByIdAndDeletedAtIsNull(f.getBookId())
                            .map(Book::getTitle).orElse(null);
                    m.put("bookTitle", title);
                    m.put("createdAt", f.getCreatedAt());
                    return m;
                }).toList();
    }

    // ---- Reviews ----
    public Map<String, Object> upsertReview(Long bookId, int rating, String comment) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        Book book = requireBook(bookId);
        if (rating < 1 || rating > 5) {
            throw new BusinessException("INVALID_RATING", "Điểm 1–5");
        }
        if (principal.getRole() == UserRole.USER
                && !borrowSlipItemRepository.existsBorrowHistoryByUserAndBook(principal.getId(), bookId)) {
            throw new BusinessException("REVIEW_NOT_ALLOWED", "Chỉ độc giả đã mượn sách mới được đánh giá");
        }
        Review review = reviewRepository.findByUser_IdAndBook_IdAndDeletedAtIsNull(principal.getId(), bookId)
                .orElseGet(Review::new);
        review.setUser(userRepository.findById(principal.getId()).orElseThrow());
        review.setBook(book);
        review.setRating(rating);
        review.setComment(comment);
        review.setStatus(ReviewStatus.VISIBLE);
        review.setUpdatedBy(principal.getId());
        if (review.getId() == null) {
            review.setCreatedBy(principal.getId());
        }
        reviewRepository.save(review);
        refreshBookRating(book);
        return reviewMap(review);
    }

    public Map<String, Object> hideReview(Long id) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy đánh giá", HttpStatus.NOT_FOUND));
        review.setStatus(ReviewStatus.HIDDEN);
        reviewRepository.save(review);
        refreshBookRating(review.getBook());
        return reviewMap(review);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> reviewsByBook(Long bookId) {
        return reviewRepository.findByBook_IdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(bookId, ReviewStatus.VISIBLE)
                .stream().map(this::reviewMap).toList();
    }

    // ---- Suggestions ----
    public Map<String, Object> suggest(String title, String author, String isbn, String reason) {
        User user = userRepository.findById(SecurityUtils.requireCurrentUser().getId()).orElseThrow();
        PurchaseSuggestion s = new PurchaseSuggestion();
        s.setUser(user);
        s.setTitle(title);
        s.setAuthor(author);
        s.setIsbn(isbn);
        s.setReason(reason);
        s.setStatus(SuggestionStatus.NEW);
        s.setCreatedBy(user.getId());
        suggestionRepository.save(s);
        return suggestionMap(s);
    }

    public Map<String, Object> updateSuggestionStatus(Long id, SuggestionStatus status) {
        PurchaseSuggestion s = suggestionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy đề xuất", HttpStatus.NOT_FOUND));
        s.setStatus(status);
        suggestionRepository.save(s);
        return suggestionMap(s);
    }

    @Transactional(readOnly = true)
    public PageResult listSuggestions(int page, int size) {
        Page<PurchaseSuggestion> result = suggestionRepository.findByDeletedAtIsNull(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult(result.map(this::suggestionMap).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private void refreshBookRating(Book book) {
        List<Review> reviews = reviewRepository.findByBook_IdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                book.getId(), ReviewStatus.VISIBLE);
        if (reviews.isEmpty()) {
            book.setAvgRating(BigDecimal.ZERO);
            book.setRatingCount(0);
        } else {
            double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
            book.setAvgRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            book.setRatingCount(reviews.size());
        }
        bookRepository.save(book);
    }

    private Book requireBook(Long id) {
        return bookRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy sách", HttpStatus.NOT_FOUND));
    }

    private String uniqueSlug(String title) {
        String base = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (!StringUtils.hasText(base)) {
            base = "tin";
        }
        String slug = base;
        int i = 1;
        while (newsRepository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }

    private Map<String, Object> newsMap(News n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("title", n.getTitle());
        m.put("slug", n.getSlug());
        m.put("coverUrl", n.getCoverUrl());
        m.put("content", n.getContent());
        m.put("status", n.getStatus());
        m.put("pinned", n.isPinned());
        m.put("publishedAt", n.getPublishedAt());
        return m;
    }

    private Map<String, Object> bannerMap(Banner b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("imageUrl", b.getImageUrl());
        m.put("link", b.getLink());
        m.put("sortOrder", b.getSortOrder());
        m.put("active", b.isActive());
        return m;
    }

    private Map<String, Object> reviewMap(Review r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("bookId", r.getBook().getId());
        m.put("userId", r.getUser().getId());
        m.put("userName", r.getUser().getFullName());
        m.put("rating", r.getRating());
        m.put("comment", r.getComment());
        m.put("status", r.getStatus());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }

    private Map<String, Object> suggestionMap(PurchaseSuggestion s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("userId", s.getUser().getId());
        m.put("title", s.getTitle());
        m.put("author", s.getAuthor());
        m.put("isbn", s.getIsbn());
        m.put("reason", s.getReason());
        m.put("status", s.getStatus());
        m.put("createdAt", s.getCreatedAt());
        return m;
    }

    public record PageResult(List<Map<String, Object>> items, int page, int size, long total) {
    }
}
