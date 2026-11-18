package com.library.fine.controller;

import com.library.common.api.ApiResponse;
import com.library.common.enums.FineStatus;
import com.library.common.enums.FineType;
import com.library.fine.service.FineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fines")
@RequiredArgsConstructor
@Tag(name = "Fines")
public class FineController {

    private final FineService fineService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách khoản phạt")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) Long readerId,
            @RequestParam(required = false) FineStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return withMeta(fineService.list(readerId, status, page, size));
    }

    @GetMapping("/me")
    @Operation(summary = "Phạt của tôi")
    public ApiResponse<List<Map<String, Object>>> me(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return withMeta(fineService.listMine(page, size));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tạo phạt thủ công")
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateReq req) {
        return ApiResponse.ok(fineService.createManual(req.getReaderId(), req.getType(), req.getAmount(), req.getReason()));
    }

    @PostMapping("/{id}/waive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Miễn/giảm phạt")
    public ApiResponse<Map<String, Object>> waive(@PathVariable Long id, @RequestBody WaiveReq req) {
        return ApiResponse.ok(fineService.waive(id, req.getReason(), req.getAmount()), "Đã miễn/giảm");
    }

    private ApiResponse<List<Map<String, Object>>> withMeta(FineService.PageResult result) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", result.page());
        meta.put("limit", result.size());
        meta.put("total", result.total());
        ApiResponse<List<Map<String, Object>>> res = ApiResponse.ok(result.items());
        res.setMeta(meta);
        return res;
    }

    @Data
    public static class CreateReq {
        private Long readerId;
        private FineType type;
        private BigDecimal amount;
        private String reason;
    }

    @Data
    public static class WaiveReq {
        private String reason;
        private BigDecimal amount;
    }
}
