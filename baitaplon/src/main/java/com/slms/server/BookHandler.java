package com.slms.server;

import com.slms.dto.ApiResponse;
import com.slms.model.Book;
import com.slms.model.BookCopy;
import com.slms.model.enums.CopyStatus;
import com.slms.service.BookService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Xử lý các API liên quan đến Đầu sách và Bản sao sách.
 */
public class BookHandler implements HttpHandler {

    private final BookService bookService = new BookService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpHelper.enableCors(exchange);
        String method = exchange.getRequestMethod().toUpperCase();

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();

        try {
            if (path.equals("/api/books")) {
                if ("GET".equals(method)) {
                    Map<String, String> params = HttpHelper.getQueryParams(exchange);
                    String query = params.get("q");
                    List<Book> books;
                    if (query != null && !query.trim().isEmpty()) {
                        books = bookService.searchBooks(query.trim());
                    } else {
                        books = bookService.getAllBooks();
                    }
                    HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(books));
                } else if ("POST".equals(method)) {
                    String body = HttpHelper.readRequestBody(exchange);
                    Book book = HttpHelper.getGson().fromJson(body, Book.class);
                    Book saved = bookService.addBook(book);
                    HttpHelper.sendJsonResponse(exchange, 201, ApiResponse.ok("Thêm sách thành công", saved));
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else if (path.equals("/api/books/copies")) {
                if ("GET".equals(method)) {
                    Map<String, String> params = HttpHelper.getQueryParams(exchange);
                    String isbn = params.get("isbn");
                    if (isbn != null && !isbn.isEmpty()) {
                        List<BookCopy> copies = bookService.getCopiesByIsbn(isbn);
                        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(copies));
                    } else {
                        HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Thiếu tham số isbn"));
                    }
                } else if ("POST".equals(method)) {
                    String body = HttpHelper.readRequestBody(exchange);
                    BookCopy copy = HttpHelper.getGson().fromJson(body, BookCopy.class);
                    if (copy.getAcquisitionDate() == null) {
                        copy.setAcquisitionDate(LocalDate.now());
                    }
                    if (copy.getStatus() == null) {
                        copy.setStatus(CopyStatus.Available);
                    }
                    BookCopy saved = bookService.addCopy(copy);
                    HttpHelper.sendJsonResponse(exchange, 201, ApiResponse.ok("Thêm bản sao sách thành công", saved));
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else if (path.startsWith("/api/books/")) {
                String isbn = path.substring("/api/books/".length());
                if ("GET".equals(method)) {
                    Optional<Book> book = bookService.getBookByIsbn(isbn);
                    if (book.isPresent()) {
                        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(book.get()));
                    } else {
                        HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Không tìm thấy sách với ISBN: " + isbn));
                    }
                } else if ("DELETE".equals(method)) {
                    boolean deleted = bookService.deleteBook(isbn);
                    if (deleted) {
                        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Xóa sách thành công", null));
                    } else {
                        HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Không thể xóa sách (có thể đang có bản sao được mượn)"));
                    }
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Endpoint không tồn tại"));
            }
        } catch (Exception e) {
            HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }
}

