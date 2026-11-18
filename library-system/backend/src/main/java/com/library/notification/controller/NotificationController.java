package com.library.notification.controller;

import com.library.common.api.ApiResponse;
import com.library.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
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
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications/me")
    @Operation(summary = "Thông báo của tôi")
    public ApiResponse<List<Map<String, Object>>> me(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        NotificationService.PageResult result = notificationService.listMine(page, size);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", result.page());
        meta.put("limit", result.size());
        meta.put("total", result.total());
        meta.put("unread", result.unread());
        ApiResponse<List<Map<String, Object>>> res = ApiResponse.ok(result.items());
        res.setMeta(meta);
        return res;
    }

    @PatchMapping("/notifications/{id}/read")
    @Operation(summary = "Đánh dấu đã đọc")
    public ApiResponse<Map<String, Object>> read(@PathVariable Long id) {
        return ApiResponse.ok(notificationService.markRead(id));
    }

    @PostMapping("/notifications/read-all")
    @Operation(summary = "Đánh dấu tất cả đã đọc")
    public ApiResponse<Map<String, Object>> readAll() {
        int n = notificationService.markAllRead();
        return ApiResponse.ok(Map.of("marked", n));
    }

    @GetMapping("/notification-templates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách mẫu thông báo")
    public ApiResponse<List<Map<String, Object>>> templates() {
        return ApiResponse.ok(notificationService.listTemplates());
    }

    @PutMapping("/notification-templates/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo/cập nhật mẫu thông báo")
    public ApiResponse<Map<String, Object>> upsertTemplate(@PathVariable String code, @RequestBody TemplateReq req) {
        return ApiResponse.ok(notificationService.upsertTemplate(
                code, req.getSubject(), req.getBody(), req.getChannel(), req.getActive()));
    }

    @Data
    public static class TemplateReq {
        private String subject;
        private String body;
        private String channel;
        private Boolean active;
    }
}
