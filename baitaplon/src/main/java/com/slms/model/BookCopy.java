package com.slms.model;

import com.slms.model.enums.CopyStatus;

import java.time.LocalDate;

/**
 * Lớp thực thể đại diện cho một bản sao cụ thể của sách.
 * Tương ứng với bảng BOOK_COPIES trong cơ sở dữ liệu.
 */
public class BookCopy {

    /** Mã bản sao - khóa chính */
    private String copyId;

    /** Mã ISBN của đầu sách liên kết */
    private String isbn;

    /** Mã vạch (barcode) duy nhất */
    private String barcode;

    /** Ngày nhập sách vào thư viện */
    private LocalDate acquisitionDate;

    /** Giá sách (VND) */
    private double price;

    /** Trạng thái hiện tại của bản sao */
    private CopyStatus status;

    /**
     * Constructor mặc định.
     */
    public BookCopy() {
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param copyId          mã bản sao
     * @param isbn            mã ISBN
     * @param barcode         mã vạch
     * @param acquisitionDate ngày nhập sách
     * @param price           giá sách
     * @param status          trạng thái
     */
    public BookCopy(String copyId, String isbn, String barcode,
                    LocalDate acquisitionDate, double price, CopyStatus status) {
        this.copyId = copyId;
        this.isbn = isbn;
        this.barcode = barcode;
        this.acquisitionDate = acquisitionDate;
        this.price = price;
        this.status = status;
    }

    // ======================== Phương thức nghiệp vụ ========================

    /**
     * Kiểm tra bản sao có sẵn sàng cho mượn hay không.
     *
     * @return true nếu trạng thái là Available
     */
    public boolean isAvailable() {
        return this.status == CopyStatus.Available;
    }

    // ======================== Getters & Setters ========================

    public String getCopyId() {
        return copyId;
    }

    public void setCopyId(String copyId) {
        this.copyId = copyId;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(LocalDate acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public CopyStatus getStatus() {
        return status;
    }

    public void setStatus(CopyStatus status) {
        this.status = status;
    }

    // ======================== toString ========================

    @Override
    public String toString() {
        return "BookCopy{" +
                "copyId='" + copyId + '\'' +
                ", isbn='" + isbn + '\'' +
                ", barcode='" + barcode + '\'' +
                ", acquisitionDate=" + acquisitionDate +
                ", price=" + price +
                ", status=" + status +
                '}';
    }
}
