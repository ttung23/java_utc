package com.library.catalog.service;

import com.library.catalog.dto.BookRequest;
import com.library.catalog.entity.Book;
import com.library.catalog.repository.BookRepository;
import com.library.common.enums.BookStatus;
import com.library.common.excel.ExcelHelper;
import com.library.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BookImportExportService {

    private final BookRepository bookRepository;
    private final BookService bookService;

    public Map<String, Object> importExcel(MultipartFile file) {
        try {
            List<Map<String, String>> rows = ExcelHelper.readSheet(file.getInputStream());
            int success = 0;
            List<Map<String, Object>> errors = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                Map<String, String> row = rows.get(i);
                int line = i + 2;
                try {
                    BookRequest req = new BookRequest();
                    req.setTitle(required(row, "title", "Title"));
                    req.setIsbn13(first(row, "isbn13", "ISBN13", "isbn"));
                    req.setIsbn10(first(row, "isbn10", "ISBN10"));
                    req.setPublishYear(parseInt(first(row, "publishYear", "year")));
                    String price = first(row, "price", "Price");
                    if (price != null && !price.isBlank()) {
                        req.setPrice(new BigDecimal(price));
                    }
                    req.setDescription(first(row, "description", "Description"));
                    req.setLoanable(!"false".equalsIgnoreCase(first(row, "loanable", "Loanable")));
                    req.setReference("true".equalsIgnoreCase(first(row, "reference", "is_reference")));
                    req.setStatus(BookStatus.ACTIVE);
                    String copies = first(row, "initialCopies", "copies");
                    if (copies != null && !copies.isBlank()) {
                        req.setInitialCopies(Integer.parseInt(copies));
                    }
                    bookService.create(req);
                    success++;
                } catch (Exception ex) {
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("line", line);
                    err.put("message", ex.getMessage());
                    errors.add(err);
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", success);
            result.put("failed", errors.size());
            result.put("errors", errors);
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("IMPORT_FAILED", "Không đọc được Excel: " + ex.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        try {
            List<Book> books = bookRepository.findAll().stream()
                    .filter(b -> b.getDeletedAt() == null)
                    .toList();
            List<String> headers = List.of("id", "title", "isbn13", "isbn10", "publishYear", "price", "status", "loanable", "reference");
            List<List<String>> rows = new ArrayList<>();
            for (Book b : books) {
                rows.add(List.of(
                        String.valueOf(b.getId()),
                        b.getTitle(),
                        nullToEmpty(b.getIsbn13()),
                        nullToEmpty(b.getIsbn10()),
                        b.getPublishYear() == null ? "" : String.valueOf(b.getPublishYear()),
                        b.getPrice() == null ? "" : b.getPrice().toPlainString(),
                        b.getStatus().name(),
                        String.valueOf(b.isLoanable()),
                        String.valueOf(b.isReference())
                ));
            }
            return ExcelHelper.writeSheet("books", headers, rows);
        } catch (Exception ex) {
            throw new BusinessException("EXPORT_FAILED", "Xuất Excel thất bại: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String required(Map<String, String> row, String... keys) {
        String v = first(row, keys);
        if (v == null || v.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "Thiếu cột title");
        }
        return v;
    }

    private String first(Map<String, String> row, String... keys) {
        for (String key : keys) {
            for (Map.Entry<String, String> e : row.entrySet()) {
                if (e.getKey().equalsIgnoreCase(key)) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    private Integer parseInt(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        return Integer.parseInt(v.replaceAll("[^0-9-]", ""));
    }

    private String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
