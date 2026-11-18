package com.library.catalog.controller;

import com.library.catalog.dto.AddCopiesRequest;
import com.library.catalog.dto.BookCopyResponse;
import com.library.catalog.dto.BookDetailResponse;
import com.library.catalog.dto.BookListItem;
import com.library.catalog.dto.BookRequest;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.service.BookCopyService;
import com.library.catalog.service.BookService;
import com.library.common.api.ApiResponse;
import com.library.common.enums.BookStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books")
public class BookController {

    private final BookService bookService;
    private final BookCopyService bookCopyService;

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
    @Operation(summary = "Sách mới (12 ACTIVE gần nhất)")
    public ApiResponse<List<BookListItem>> listNew() {
        return ApiResponse.ok(bookService.listNew());
    }

    @GetMapping("/popular")
    @Operation(summary = "Sách phổ biến (top 12 borrowCount)")
    public ApiResponse<List<BookListItem>> listPopular() {
        return ApiResponse.ok(bookService.listPopular());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết sách")
    public ApiResponse<BookDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(bookService.getById(id));
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
    public ApiResponse<BookDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest request
    ) {
        return ApiResponse.ok(bookService.update(id, request), "Cập nhật sách thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa mềm sách (ADMIN)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ApiResponse.ok(null, "Ẩn/xóa sách thành công");
    }

    @GetMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách bản sao của sách")
    public ApiResponse<List<BookCopyResponse>> listCopies(@PathVariable Long id) {
        return ApiResponse.ok(bookCopyService.listByBook(id));
    }

    @PostMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Thêm bản sao (sinh barcode)")
    public ApiResponse<List<BookCopyResponse>> addCopies(
            @PathVariable Long id,
            @Valid @RequestBody AddCopiesRequest request
    ) {
        return ApiResponse.ok(bookCopyService.addCopies(id, request), "Thêm bản sao thành công");
    }
}
