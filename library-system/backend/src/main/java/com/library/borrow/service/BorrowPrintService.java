package com.library.borrow.service;

import com.library.borrow.entity.BorrowSlip;
import com.library.borrow.entity.BorrowSlipItem;
import com.library.borrow.repository.BorrowSlipItemRepository;
import com.library.borrow.repository.BorrowSlipRepository;
import com.library.common.exception.BusinessException;
import com.library.common.pdf.PdfSupport;
import com.library.settings.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BorrowPrintService {

    private final BorrowSlipRepository borrowSlipRepository;
    private final BorrowSlipItemRepository borrowSlipItemRepository;
    private final SettingRepository settingRepository;

    public byte[] printSlipPdf(Long slipId) {
        BorrowSlip slip = borrowSlipRepository.findByIdAndDeletedAtIsNull(slipId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy phiếu", HttpStatus.NOT_FOUND));
        List<BorrowSlipItem> items = borrowSlipItemRepository.findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(slipId);
        List<String[]> rows = new ArrayList<>();
        for (BorrowSlipItem item : items) {
            rows.add(new String[]{
                    item.getBook().getTitle(),
                    item.getCopy() != null ? item.getCopy().getBarcode() : "-",
                    item.getDueDate() != null ? item.getDueDate().toString() : "-"
            });
        }
        String libraryName = settingRepository.findByKey("library_name")
                .map(s -> s.getValue().asText("Thư viện"))
                .orElse("Thư viện");
        try {
            return PdfSupport.borrowSlipPdf(
                    libraryName,
                    slip.getSlipCode(),
                    slip.getReader().getUser().getFullName(),
                    slip.getReader().getReaderCode(),
                    rows
            );
        } catch (Exception ex) {
            throw new BusinessException("PDF_FAILED", "In phiếu thất bại: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
