package uz.eduplatform.core.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.utils.MessageService;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Fayllar", description = "Fayl yuklash va yuklab olish API'lari")
public class FileController {

    private final FileStorageService storageService;
    private final MessageService messageService;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Fayl yuklash", description = "Rasm faylini serverga yuklash. Maksimal hajm: 5MB. Faqat rasm formatlari (JPG, PNG, GIF) qabul qilinadi.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        FileStorageService.FileInfo info = storageService.store(file);

        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/")
                .path(info.storedFilename())
                .toUriString();

        Map<String, Object> response = Map.of(
                "filename", info.storedFilename(),
                "originalFilename", info.originalFilename(),
                "contentType", info.contentType(),
                "size", info.size(),
                "url", downloadUrl
        );

        return ResponseEntity.ok(ApiResponse.success(response, messageService.get("file.uploaded")));
    }

    @GetMapping("/{filename}")
    @Operation(summary = "Faylni yuklab olish", description = "Serverdan faylni nomi bo'yicha yuklab olish.")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        Path filePath = storageService.load(filename);

        try {
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = java.nio.file.Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw BusinessException.ofKey("file.not.found");
        } catch (java.io.IOException e) {
            throw BusinessException.ofKey("file.content.type.error");
        }
    }

    @DeleteMapping("/{filename}")
    @Operation(summary = "Faylni o'chirish", description = "Serverdan faylni o'chirish. Faqat TEACHER, ADMIN va SUPER_ADMIN rollari uchun.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String filename) {
        storageService.delete(filename);
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("file.deleted")));
    }
}
