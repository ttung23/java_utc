package com.slms.model;

/**
 * Lớp thực thể đại diện cho Thể loại sách trong thư viện.
 * Tương ứng với bảng CATEGORIES trong cơ sở dữ liệu.
 * Quan hệ 1:N với bảng BOOKS (Một thể loại có nhiều đầu sách).
 */
public class Category {

    /** Mã thể loại - Khóa chính */
    private String categoryId;

    /** Tên thể loại (duy nhất) */
    private String categoryName;

    /** Mô tả thể loại */
    private String description;

    public Category() {
    }

    public Category(String categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public Category(String categoryId, String categoryName, String description) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Category{" +
                "categoryId='" + categoryId + '\'' +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }
}

