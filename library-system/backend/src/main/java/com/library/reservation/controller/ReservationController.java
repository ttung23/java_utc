package com.library.reservation.controller;

import com.library.common.api.ApiResponse;
import com.library.reservation.service.ReservationService;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Đặt trước sách")
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateReq req) {
        return ApiResponse.ok(reservationService.create(req.getBookId(), req.getReaderId()), "Đã đặt trước");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách đặt trước")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) Long readerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return withMeta(reservationService.list(readerId, page, size));
    }

    @GetMapping("/me")
    @Operation(summary = "Đặt trước của tôi")
    public ApiResponse<List<Map<String, Object>>> me(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return withMeta(reservationService.listMine(page, size));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy đặt trước")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long id) {
        return ApiResponse.ok(reservationService.cancel(id), "Đã hủy đặt trước");
    }

    private ApiResponse<List<Map<String, Object>>> withMeta(ReservationService.PageResult result) {
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
        private Long bookId;
        private Long readerId;
    }
}
