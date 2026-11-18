package com.library.cms.controller;

import com.library.cms.service.CmsService;
import com.library.common.api.ApiResponse;
import com.library.common.enums.NewsStatus;
import com.library.common.enums.SuggestionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "CMS")
public class CmsController {

    private final CmsService cmsService;

    @GetMapping("/news/public")
    @Operation(summary = "Tin đã đăng (public)")
    public ApiResponse<List<Map<String, Object>>> publicNews() {
        return ApiResponse.ok(cmsService.publicNews());
    }

    @GetMapping("/news/slug/{slug}")
    public ApiResponse<Map<String, Object>> bySlug(@PathVariable String slug) {
        return ApiResponse.ok(cmsService.newsBySlug(slug));
    }

    @GetMapping("/news")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<List<Map<String, Object>>> listNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return page(cmsService.listNews(page, size));
    }

    @PostMapping("/news")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<Map<String, Object>> createNews(@RequestBody NewsReq req) {
        return ApiResponse.ok(cmsService.createNews(req.getTitle(), req.getContent(), req.getCoverUrl(), req.getPinned(), req.getStatus()));
    }

    @PutMapping("/news/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<Map<String, Object>> updateNews(@PathVariable Long id, @RequestBody NewsReq req) {
        return ApiResponse.ok(cmsService.updateNews(id, req.getTitle(), req.getContent(), req.getCoverUrl(), req.getPinned(), req.getStatus()));
    }

    @DeleteMapping("/news/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteNews(@PathVariable Long id) {
        cmsService.deleteNews(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/banners/public")
    public ApiResponse<List<Map<String, Object>>> publicBanners() {
        return ApiResponse.ok(cmsService.publicBanners());
    }

    @GetMapping("/banners")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<List<Map<String, Object>>> listBanners() {
        return ApiResponse.ok(cmsService.listBanners());
    }

    @PostMapping("/banners")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<Map<String, Object>> createBanner(@RequestBody BannerReq req) {
        return ApiResponse.ok(cmsService.createBanner(req.getImageUrl(), req.getLink(), req.getSortOrder(), req.getActive()));
    }

    @PutMapping("/banners/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<Map<String, Object>> updateBanner(@PathVariable Long id, @RequestBody BannerReq req) {
        return ApiResponse.ok(cmsService.updateBanner(id, req.getImageUrl(), req.getLink(), req.getSortOrder(), req.getActive()));
    }

    @DeleteMapping("/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteBanner(@PathVariable Long id) {
        cmsService.deleteBanner(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/favorites/me")
    public ApiResponse<List<Map<String, Object>>> myFavorites() {
        return ApiResponse.ok(cmsService.myFavorites());
    }

    @PostMapping("/favorites/{bookId}")
    public ApiResponse<Void> addFavorite(@PathVariable Long bookId) {
        cmsService.addFavorite(bookId);
        return ApiResponse.ok(null, "Đã thêm yêu thích");
    }

    @DeleteMapping("/favorites/{bookId}")
    public ApiResponse<Void> removeFavorite(@PathVariable Long bookId) {
        cmsService.removeFavorite(bookId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/reviews/me")
    public ApiResponse<List<Map<String, Object>>> myReviews() {
        return ApiResponse.ok(cmsService.myReviews());
    }

    @GetMapping("/reviews")
    public ApiResponse<List<Map<String, Object>>> reviews(@RequestParam Long bookId) {
        return ApiResponse.ok(cmsService.reviewsByBook(bookId));
    }

    @PostMapping("/reviews")
    public ApiResponse<Map<String, Object>> upsertReview(@RequestBody ReviewReq req) {
        return ApiResponse.ok(cmsService.upsertReview(req.getBookId(), req.getRating(), req.getComment()));
    }

    @PostMapping("/reviews/{id}/hide")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<Map<String, Object>> hideReview(@PathVariable Long id) {
        return ApiResponse.ok(cmsService.hideReview(id));
    }

    @PostMapping("/purchase-suggestions")
    public ApiResponse<Map<String, Object>> suggest(@RequestBody SuggestReq req) {
        return ApiResponse.ok(cmsService.suggest(req.getTitle(), req.getAuthor(), req.getIsbn(), req.getReason()));
    }

    @GetMapping("/purchase-suggestions")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<List<Map<String, Object>>> listSuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return page(cmsService.listSuggestions(page, size));
    }

    @PostMapping("/purchase-suggestions/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<Map<String, Object>> suggestionStatus(@PathVariable Long id, @RequestBody StatusReq req) {
        return ApiResponse.ok(cmsService.updateSuggestionStatus(id, req.getStatus()));
    }

    private ApiResponse<List<Map<String, Object>>> page(CmsService.PageResult result) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", result.page());
        meta.put("limit", result.size());
        meta.put("total", result.total());
        ApiResponse<List<Map<String, Object>>> res = ApiResponse.ok(result.items());
        res.setMeta(meta);
        return res;
    }

    @Data
    public static class NewsReq {
        private String title;
        private String content;
        private String coverUrl;
        private Boolean pinned;
        private NewsStatus status;
    }

    @Data
    public static class BannerReq {
        private String imageUrl;
        private String link;
        private Integer sortOrder;
        private Boolean active;
    }

    @Data
    public static class ReviewReq {
        private Long bookId;
        private int rating;
        private String comment;
    }

    @Data
    public static class SuggestReq {
        private String title;
        private String author;
        private String isbn;
        private String reason;
    }

    @Data
    public static class StatusReq {
        private SuggestionStatus status;
    }
}
