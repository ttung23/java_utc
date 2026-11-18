package com.library.common.storage;

import com.library.common.config.UploadProperties;
import com.library.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> COVER_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UploadProperties uploadProperties;

    public String storeCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "File trống");
        }
        if (file.getSize() > uploadProperties.getMaxCoverBytes()) {
            throw new BusinessException("FILE_TOO_LARGE", "Ảnh bìa tối đa 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !COVER_TYPES.contains(contentType)) {
            throw new BusinessException("INVALID_FILE_TYPE", "Chỉ nhận jpg/png/webp");
        }
        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        try {
            Path root = Paths.get(uploadProperties.getDir(), "covers").toAbsolutePath().normalize();
            Files.createDirectories(root);
            String name = UUID.randomUUID() + ext;
            Path target = root.resolve(name);
            Files.copy(file.getInputStream(), target);
            return "/uploads/covers/" + name;
        } catch (IOException e) {
            throw new BusinessException("UPLOAD_FAILED", "Không lưu được file: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Path resolvePublicPath(String publicPath) {
        if (!StringUtils.hasText(publicPath) || !publicPath.startsWith("/uploads/")) {
            throw new BusinessException("INVALID_PATH", "Đường dẫn không hợp lệ");
        }
        return Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize()
                .resolve(publicPath.replaceFirst("^/uploads/", ""));
    }
}
