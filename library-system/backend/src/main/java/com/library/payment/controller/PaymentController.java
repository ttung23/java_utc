package com.library.payment.controller;

import com.library.common.api.ApiResponse;
import com.library.common.enums.PaymentMethod;
import com.library.payment.service.PaymentService;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách biên lai")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) Long readerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PaymentService.PageResult result = paymentService.list(readerId, page, size);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", result.page());
        meta.put("limit", result.size());
        meta.put("total", result.total());
        ApiResponse<List<Map<String, Object>>> res = ApiResponse.ok(result.items());
        res.setMeta(meta);
        return res;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Chi tiết biên lai")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.ok(paymentService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Thu tiền phạt")
    public ApiResponse<Map<String, Object>> collect(@RequestBody CollectReq req) {
        return ApiResponse.ok(paymentService.collect(
                req.getReaderId(), req.getFineIds(), req.getAmount(), req.getMethod(), req.getNote()),
                "Thu tiền thành công");
    }

    @PostMapping("/request")
    @Operation(summary = "USER gửi yêu cầu nộp phạt")
    public ApiResponse<Map<String, Object>> request(@RequestBody RequestReq req) {
        return ApiResponse.ok(paymentService.requestPayment(req.getFineIds(), req.getAmount(), req.getNote()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Xác nhận yêu cầu nộp")
    public ApiResponse<Map<String, Object>> confirm(@PathVariable Long id) {
        return ApiResponse.ok(paymentService.confirm(id), "Đã xác nhận");
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hoàn tiền")
    public ApiResponse<Map<String, Object>> refund(@PathVariable Long id, @RequestBody RefundReq req) {
        return ApiResponse.ok(paymentService.refund(id, req.getNote()), "Đã hoàn tiền");
    }

    @Data
    public static class CollectReq {
        private Long readerId;
        private List<Long> fineIds;
        private BigDecimal amount;
        private PaymentMethod method;
        private String note;
    }

    @Data
    public static class RequestReq {
        private List<Long> fineIds;
        private BigDecimal amount;
        private String note;
    }

    @Data
    public static class RefundReq {
        private String note;
    }
}
