package uz.eduplatform.core.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import uz.eduplatform.core.common.exception.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    private final Path uploadDir;

    public FileStorageService(@Value("${app.storage.upload-dir:./uploads}") String uploadPath) {
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + uploadPath, e);
        }
    }

    public FileInfo store(MultipartFile file) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");

        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            Path targetPath = uploadDir.resolve(storedFilename);
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored: {} -> {} ({} bytes)", originalFilename, storedFilename, file.getSize());

            return new FileInfo(
                    storedFilename,
                    originalFilename,
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException e) {
            throw BusinessException.ofKey("storage.file.store.failed");
        }
    }

    public Path load(String filename) {
        Path filePath = uploadDir.resolve(filename).normalize();
        if (!filePath.startsWith(uploadDir)) {
            throw BusinessException.ofKey("file.invalid.path");
        }
        if (!Files.exists(filePath)) {
            throw BusinessException.ofKey("file.not.found");
        }
        return filePath;
    }

    public boolean delete(String filename) {
        try {
            Path filePath = uploadDir.resolve(filename).normalize();
            if (!filePath.startsWith(uploadDir)) {
                return false;
            }
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filename, e);
            return false;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.ofKey("file.empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.ofKey("file.too.large");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw BusinessException.ofKey("file.type.not.allowed");
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(dotIndex);
        }
        return "";
    }

    public record FileInfo(String storedFilename, String originalFilename, String contentType, long size) {
    }
}
