package com.slms.model;

/**
 * Lớp thực thể đại diện cho một đầu sách trong thư viện.
 * Tương ứng với bảng BOOKS trong cơ sở dữ liệu.
 */
public class Book {

    /** Mã ISBN - khóa chính */
    private String isbn;

    /** Tiêu đề sách */
    private String title;

    /** Tác giả */
    private String author;

    /** Nhà xuất bản */
    private String publisher;

    /** Năm xuất bản */
    private int publishYear;

    /** Thể loại sách */
    private String category;

    /**
     * Constructor mặc định.
     */
    public Book() {
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param isbn        mã ISBN
     * @param title       tiêu đề sách
     * @param author      tác giả
     * @param publisher   nhà xuất bản
     * @param publishYear năm xuất bản
     * @param category    thể loại
     */
    public Book(String isbn, String title, String author, String publisher,
                int publishYear, String category) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.category = category;
    }

    // ======================== Getters & Setters ========================

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public int getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(int publishYear) {
        this.publishYear = publishYear;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // ======================== toString ========================

    @Override
    public String toString() {
        return "Book{" +
                "isbn='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publishYear=" + publishYear +
                ", category='" + category + '\'' +
                '}';
    }
}
