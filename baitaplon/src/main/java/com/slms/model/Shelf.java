package com.slms.model;

/**
 * Lớp thực thể đại diện cho Kệ sách / Quầy lưu kho trong thư viện.
 * Tương ứng với bảng SHELVES trong cơ sở dữ liệu.
 * Quan hệ 1:N với bảng BOOKS (Một kệ có thể chứa nhiều đầu sách).
 */
public class Shelf {

    /** Mã kệ - Khóa chính (VD: SH001, SH002) */
    private String shelfId;

    /** Tên kệ / Vị trí lưu kho (duy nhất, VD: Kệ B1 - Tầng 2 (CNTT)) */
    private String shelfName;

    /** Mô tả hướng dẫn vị trí thực tế */
    private String description;

    /** Số lượng đầu sách hiện đang đặt tại kệ này */
    private int bookCount;

    public Shelf() {
    }

    public Shelf(String shelfId, String shelfName) {
        this.shelfId = shelfId;
        this.shelfName = shelfName;
    }

    public Shelf(String shelfId, String shelfName, String description) {
        this.shelfId = shelfId;
        this.shelfName = shelfName;
        this.description = description;
    }

    public String getShelfId() {
        return shelfId;
    }

    public void setShelfId(String shelfId) {
        this.shelfId = shelfId;
    }

    public String getShelfName() {
        return shelfName;
    }

    public void setShelfName(String shelfName) {
        this.shelfName = shelfName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getBookCount() {
        return bookCount;
    }

    public void setBookCount(int bookCount) {
        this.bookCount = bookCount;
    }

    @Override
    public String toString() {
        return "Shelf{" +
                "shelfId='" + shelfId + '\'' +
                ", shelfName='" + shelfName + '\'' +
                ", bookCount=" + bookCount +
                '}';
    }
}

