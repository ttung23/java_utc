package com.slms.service;

import com.slms.model.Shelf;
import com.slms.repository.ShelfRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service xử lý các nghiệp vụ liên quan đến Kệ sách / Vị trí lưu kho (Shelves).
 */
public class ShelfService {

    private final ShelfRepository shelfRepository;

    public ShelfService() {
        this.shelfRepository = new ShelfRepository();
    }

    public ShelfService(ShelfRepository shelfRepository) {
        this.shelfRepository = shelfRepository;
    }

    /**
     * Lấy toàn bộ danh sách kệ sách.
     */
    public List<Shelf> getAllShelves() {
        return shelfRepository.findAll();
    }

    /**
     * Tìm kệ sách theo ID.
     */
    public Optional<Shelf> getShelfById(String shelfId) {
        return shelfRepository.findById(shelfId);
    }

    /**
     * Thêm kệ sách mới.
     *
     * @param shelf thông tin kệ cần thêm
     * @return kệ sách đã lưu
     */
    public Shelf addShelf(Shelf shelf) {
        if (shelf == null || shelf.getShelfName() == null || shelf.getShelfName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên kệ không được để trống.");
        }

        String name = shelf.getShelfName().trim();
        Optional<Shelf> existing = shelfRepository.findByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }

        shelf.setShelfName(name);
        return shelfRepository.save(shelf);
    }

    /**
     * Lấy hoặc tạo mới kệ theo tên.
     */
    public Shelf getOrCreateByName(String shelfName) {
        return shelfRepository.getOrCreateByName(shelfName);
    }

    /**
     * Cập nhật thông tin kệ sách.
     */
    public boolean updateShelf(Shelf shelf) {
        if (shelf == null || shelf.getShelfId() == null || shelf.getShelfId().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã kệ không hợp lệ.");
        }
        if (shelf.getShelfName() == null || shelf.getShelfName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên kệ không được để trống.");
        }
        return shelfRepository.update(shelf);
    }

    /**
     * Xóa kệ sách (sẽ từ chối nếu có sách đang đặt tại kệ).
     */
    public boolean deleteShelf(String shelfId) {
        if (shelfId == null || shelfId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã kệ không hợp lệ.");
        }
        return shelfRepository.delete(shelfId);
    }
}

