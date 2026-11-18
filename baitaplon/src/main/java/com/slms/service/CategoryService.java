package com.slms.service;

import com.slms.model.Category;
import com.slms.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service xử lý các nghiệp vụ liên quan đến Thể loại sách (Categories).
 */
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService() {
        this.categoryRepository = new CategoryRepository();
    }

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Lấy toàn bộ danh sách thể loại sách.
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Tìm thể loại theo ID.
     */
    public Optional<Category> getCategoryById(String categoryId) {
        return categoryRepository.findById(categoryId);
    }

    /**
     * Thêm thể loại mới.
     *
     * @param category thông tin thể loại cần thêm
     * @return thể loại đã lưu
     */
    public Category addCategory(Category category) {
        if (category == null || category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thể loại không được để trống.");
        }

        String name = category.getCategoryName().trim();
        Optional<Category> existing = categoryRepository.findByName(name);
        if (existing.isPresent()) {
            return existing.get(); // Nếu đã tồn tại thì trả về thể loại sẵn có
        }

        category.setCategoryName(name);
        return categoryRepository.save(category);
    }

    /**
     * Lấy hoặc tạo mới thể loại theo tên.
     */
    public Category getOrCreateByName(String categoryName) {
        return categoryRepository.getOrCreateByName(categoryName);
    }

    /**
     * Cập nhật thông tin thể loại.
     */
    public boolean updateCategory(Category category) {
        if (category == null || category.getCategoryId() == null || category.getCategoryId().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã thể loại không hợp lệ.");
        }
        if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thể loại không được để trống.");
        }
        return categoryRepository.update(category);
    }

    /**
     * Xóa thể loại (sẽ từ chối nếu có sách liên kết).
     */
    public boolean deleteCategory(String categoryId) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã thể loại không hợp lệ.");
        }
        return categoryRepository.delete(categoryId);
    }
}

