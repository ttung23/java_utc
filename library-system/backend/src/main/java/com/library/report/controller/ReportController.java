package com.library.report.controller;

import com.library.common.api.ApiResponse;
import com.library.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard KPI")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(reportService.dashboard());
    }

    @GetMapping("/overdue")
    @Operation(summary = "Báo cáo quá hạn")
    public ApiResponse<Map<String, Object>> overdue() {
        return ApiResponse.ok(reportService.overdueReport());
    }

    @GetMapping("/fines")
    @Operation(summary = "Tóm tắt phạt")
    public ApiResponse<Map<String, Object>> fines() {
        return ApiResponse.ok(reportService.finesSummary());
    }

    @GetMapping("/top-books")
    @Operation(summary = "Top sách mượn nhiều")
    public ApiResponse<Map<String, Object>> topBooks(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(reportService.topBooks(limit));
    }

    @GetMapping("/inventory")
    @Operation(summary = "Tóm tắt tồn kho theo trạng thái")
    public ApiResponse<Map<String, Object>> inventory() {
        return ApiResponse.ok(reportService.inventorySummary());
    }

    @GetMapping("/overdue/export")
    @Operation(summary = "Xuất Excel danh sách quá hạn")
    public ResponseEntity<byte[]> exportOverdue() {
        byte[] data = reportService.exportOverdueExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=overdue.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
