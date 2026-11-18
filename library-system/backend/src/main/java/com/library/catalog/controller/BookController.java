package com.library.catalog.controller;

import com.library.catalog.dto.AddCopiesRequest;
import com.library.catalog.dto.BookCopyResponse;
import com.library.catalog.dto.BookDetailResponse;
import com.library.catalog.dto.BookListItem;
import com.library.catalog.dto.BookRequest;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.service.BookCopyService;
import com.library.catalog.service.BookImportExportService;
import com.library.catalog.service.BookService;
import com.library.common.api.ApiResponse;
import com.library.common.enums.BookStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books")
public class BookController {

    private final BookService bookService;
    private final BookCopyService bookCopyService;
    private final BookImportExportService bookImportExportService;

    @GetMapping
    @Operation(summary = "Danh sách sách (public)")
    public ApiResponse<List<BookListItem>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long author,
            @RequestParam(required = false) Long publisher,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long language,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) BookStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        PagedResult<BookListItem> result = bookService.list(
                q, category, author, publisher, year, language, available, status, page, size, sort
        );
        ApiResponse<List<BookListItem>> response = ApiResponse.ok(result.getItems());
        response.setMeta(result.meta());
        return response;
    }

    @GetMapping("/new")
    @Operation(summary = "Sách mới")
    public ApiResponse<List<BookListItem>> listNew() {
        return ApiResponse.ok(bookService.listNew());
    }

    @GetMapping("/popular")
    @Operation(summary = "Sách phổ biến")
    public ApiResponse<List<BookListItem>> listPopular() {
        return ApiResponse.ok(bookService.listPopular());
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Xuất Excel đầu sách")
    public ResponseEntity<byte[]> export() {
        byte[] data = bookImportExportService.exportExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=books.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Import Excel đầu sách")
    public ApiResponse<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(bookImportExportService.importExcel(file), "Import xong");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết sách")
    public ApiResponse<BookDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(bookService.getById(id));
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "Sách liên quan")
    public ApiResponse<List<BookListItem>> related(@PathVariable Long id) {
        return ApiResponse.ok(bookService.related(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tạo đầu sách")
    public ApiResponse<BookDetailResponse> create(@Valid @RequestBody BookRequest request) {
        return ApiResponse.ok(bookService.create(request), "Tạo sách thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Cập nhật đầu sách")
    public ApiResponse<BookDetailResponse> update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return ApiResponse.ok(bookService.update(id, request), "Cập nhật sách thành công");
    }

    @PostMapping("/{id}/cover")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Upload ảnh bìa")
    public ApiResponse<BookDetailResponse> uploadCover(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(bookService.uploadCover(id, file), "Upload bìa thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa mềm sách")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ApiResponse.ok(null, "Ẩn/xóa sách thành công");
    }

    @GetMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách bản sao")
    public ApiResponse<List<BookCopyResponse>> listCopies(@PathVariable Long id) {
        return ApiResponse.ok(bookCopyService.listByBook(id));
    }

    @PostMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Thêm bản sao")
    public ApiResponse<List<BookCopyResponse>> addCopies(
            @PathVariable Long id,
            @Valid @RequestBody AddCopiesRequest request
    ) {
        return ApiResponse.ok(bookCopyService.addCopies(id, request), "Thêm bản sao thành công");
    }
}
