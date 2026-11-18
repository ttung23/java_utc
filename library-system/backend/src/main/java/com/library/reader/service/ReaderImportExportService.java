package com.library.reader.service;

import com.library.common.excel.ExcelHelper;
import com.library.common.exception.BusinessException;
import com.library.reader.dto.CreateReaderRequest;
import com.library.reader.entity.Reader;
import com.library.reader.entity.ReaderType;
import com.library.reader.repository.ReaderRepository;
import com.library.reader.repository.ReaderTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ReaderImportExportService {

    private final ReaderService readerService;
    private final ReaderRepository readerRepository;
    private final ReaderTypeRepository readerTypeRepository;

    public Map<String, Object> importExcel(MultipartFile file) {
        try {
            List<Map<String, String>> rows = ExcelHelper.readSheet(file.getInputStream());
            int success = 0;
            List<Map<String, Object>> errors = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                Map<String, String> row = rows.get(i);
                int line = i + 2;
                try {
                    CreateReaderRequest req = new CreateReaderRequest();
                    req.setFullName(required(row, "fullName", "full_name", "name"));
                    req.setEmail(required(row, "email"));
                    req.setPhone(first(row, "phone"));
                    String typeCode = first(row, "readerType", "reader_type", "type");
                    ReaderType type = readerTypeRepository.findAll().stream()
                            .filter(t -> t.getDeletedAt() == null)
                            .filter(t -> typeCode == null || typeCode.isBlank() || t.getCode().equalsIgnoreCase(typeCode))
                            .findFirst()
                            .orElseThrow(() -> new BusinessException("READER_TYPE_NOT_FOUND", "Không tìm thấy loại độc giả"));
                    req.setReaderTypeId(type.getId());
                    req.setStudentCode(first(row, "studentCode", "student_code"));
                    req.setIdNumber(first(row, "idNumber", "id_number", "cccd"));
                    req.setAddress(first(row, "address"));
                    readerService.create(req);
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
        } catch (Exception ex) {
            throw new BusinessException("IMPORT_FAILED", "Không đọc được Excel: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        try {
            List<Reader> readers = readerRepository.findAll().stream()
                    .filter(r -> r.getDeletedAt() == null)
                    .toList();
            List<String> headers = List.of("id", "readerCode", "fullName", "email", "phone", "readerType", "blacklisted");
            List<List<String>> rows = new ArrayList<>();
            for (Reader r : readers) {
                rows.add(List.of(
                        String.valueOf(r.getId()),
                        nullToEmpty(r.getReaderCode()),
                        r.getUser().getFullName(),
                        r.getUser().getEmail(),
                        nullToEmpty(r.getUser().getPhone()),
                        r.getReaderType().getCode(),
                        String.valueOf(r.isBlacklisted())
                ));
            }
            return ExcelHelper.writeSheet("readers", headers, rows);
        } catch (Exception ex) {
            throw new BusinessException("EXPORT_FAILED", "Xuất Excel thất bại: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String required(Map<String, String> row, String... keys) {
        String v = first(row, keys);
        if (v == null || v.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "Thiếu trường bắt buộc");
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

    private String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
