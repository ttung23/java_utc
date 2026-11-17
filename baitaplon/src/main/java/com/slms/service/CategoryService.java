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
}

