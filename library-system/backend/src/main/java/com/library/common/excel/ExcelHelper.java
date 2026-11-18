package com.library.common.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExcelHelper {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private ExcelHelper() {
    }

    public static List<Map<String, String>> readSheet(InputStream in) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) {
                return List.of();
            }
            List<String> keys = new ArrayList<>();
            for (Cell cell : header) {
                keys.add(FORMATTER.formatCellValue(cell).trim());
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, String> map = new LinkedHashMap<>();
                boolean empty = true;
                for (int c = 0; c < keys.size(); c++) {
                    Cell cell = row.getCell(c);
                    String value = cell == null ? "" : FORMATTER.formatCellValue(cell).trim();
                    if (!value.isEmpty()) {
                        empty = false;
                    }
                    map.put(keys.get(c), value);
                }
                if (!empty) {
                    rows.add(map);
                }
            }
            return rows;
        }
    }

    public static byte[] writeSheet(String sheetName, List<String> headers, List<List<String>> rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<String> values = rows.get(r);
                for (int c = 0; c < values.size(); c++) {
                    row.createCell(c).setCellValue(values.get(c) == null ? "" : values.get(c));
                }
            }
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
