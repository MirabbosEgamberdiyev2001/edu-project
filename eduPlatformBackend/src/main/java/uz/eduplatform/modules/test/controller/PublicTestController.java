package uz.eduplatform.modules.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.modules.test.dto.TestHistoryDto;
import uz.eduplatform.modules.test.service.TestHistoryService;

@RestController
@RequestMapping("/api/v1/public-tests")
@RequiredArgsConstructor
@Tag(name = "Ochiq testlar", description = "Autentifikatsiyasiz ochiq testlar API")
public class PublicTestController {

    private final TestHistoryService historyService;

    @GetMapping("/{slug}")
    @Operation(summary = "Ochiq test ma'lumotini olish", description = "Slug bo'yicha ochiq test haqida ma'lumot olish.")
    public ResponseEntity<ApiResponse<TestHistoryDto>> getPublicTest(@PathVariable String slug) {
        TestHistoryDto dto = historyService.getPublicTestBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
