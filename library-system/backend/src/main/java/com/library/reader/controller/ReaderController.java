package com.library.reader.controller;

import com.library.common.api.ApiResponse;
import com.library.common.enums.CardStatus;
import com.library.reader.dto.BlacklistRequest;
import com.library.reader.dto.BlockReaderRequest;
import com.library.reader.dto.CreateReaderRequest;
import com.library.reader.dto.EligibilityResponse;
import com.library.reader.dto.PagedResult;
import com.library.reader.dto.ReaderDetailResponse;
import com.library.reader.dto.ReaderListItem;
import com.library.reader.dto.UpdateReaderRequest;
import com.library.reader.service.ReaderActivityService;
import com.library.reader.service.ReaderImportExportService;
import com.library.reader.service.ReaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/readers")
@RequiredArgsConstructor
@Tag(name = "Readers")
public class ReaderController {

    private final ReaderService readerService;
    private final ReaderActivityService readerActivityService;
    private final ReaderImportExportService readerImportExportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách độc giả")
    public ApiResponse<List<ReaderListItem>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long readerTypeId,
            @RequestParam(required = false) CardStatus cardStatus,
            @RequestParam(required = false) Boolean blacklisted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        PagedResult<ReaderListItem> result = readerService.list(
                q, readerTypeId, cardStatus, blacklisted, page, size, sort);
        ApiResponse<List<ReaderListItem>> response = ApiResponse.ok(result.getItems());
        response.setMeta(result.meta());
        return response;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hồ sơ độc giả của tôi")
    public ApiResponse<ReaderDetailResponse> me() {
        return ApiResponse.ok(readerService.getMe());
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tra cứu độc giả theo mã thẻ / barcode")
    public ApiResponse<ReaderDetailResponse> lookup(@RequestParam String card) {
        return ApiResponse.ok(readerService.lookupByCard(card));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Xuất Excel độc giả")
    public ResponseEntity<byte[]> export() {
        byte[] data = readerImportExportService.exportExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=readers.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Import Excel độc giả")
    public ApiResponse<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(readerImportExportService.importExcel(file), "Import xong");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Chi tiết độc giả")
    public ApiResponse<ReaderDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(readerService.getById(id));
    }

    @GetMapping("/{id}/eligibility")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Kiểm tra điều kiện mượn (core)")
    public ApiResponse<EligibilityResponse> eligibility(@PathVariable Long id) {
        return ApiResponse.ok(readerService.eligibility(id));
    }

    @GetMapping("/{id}/borrowing")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Sách đang mượn của độc giả")
    public ApiResponse<List<Map<String, Object>>> borrowing(@PathVariable Long id) {
        return ApiResponse.ok(readerActivityService.borrowing(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Lịch sử phiếu mượn")
    public ApiResponse<List<Map<String, Object>>> history(@PathVariable Long id) {
        return ApiResponse.ok(readerActivityService.history(id));
    }

    @GetMapping("/{id}/fines")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Khoản phạt của độc giả")
    public ApiResponse<List<Map<String, Object>>> fines(@PathVariable Long id) {
        return ApiResponse.ok(readerActivityService.fines(id));
    }

    @GetMapping("/{id}/reservations")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Đặt trước của độc giả")
    public ApiResponse<List<Map<String, Object>>> reservations(@PathVariable Long id) {
        return ApiResponse.ok(readerActivityService.reservations(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Đăng ký độc giả tại quầy")
    public ApiResponse<ReaderDetailResponse> create(@Valid @RequestBody CreateReaderRequest request) {
        return ApiResponse.ok(readerService.create(request), "Tạo độc giả thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Cập nhật hồ sơ độc giả")
    public ApiResponse<ReaderDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReaderRequest request
    ) {
        return ApiResponse.ok(readerService.update(id, request), "Cập nhật độc giả thành công");
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Khóa tài khoản độc giả")
    public ApiResponse<ReaderDetailResponse> block(
            @PathVariable Long id,
            @RequestBody(required = false) BlockReaderRequest request
    ) {
        return ApiResponse.ok(readerService.block(id, request), "Đã khóa tài khoản");
    }

    @PatchMapping("/{id}/unblock")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Mở khóa tài khoản độc giả")
    public ApiResponse<ReaderDetailResponse> unblock(@PathVariable Long id) {
        return ApiResponse.ok(readerService.unblock(id), "Đã mở khóa tài khoản");
    }

    @PatchMapping("/{id}/blacklist")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Đưa vào / gỡ danh sách đen")
    public ApiResponse<ReaderDetailResponse> blacklist(
            @PathVariable Long id,
            @Valid @RequestBody BlacklistRequest request
    ) {
        String msg = Boolean.TRUE.equals(request.getBlacklisted())
                ? "Đã đưa vào danh sách đen"
                : "Đã gỡ danh sách đen";
        return ApiResponse.ok(readerService.blacklist(id, request), msg);
    }
}
