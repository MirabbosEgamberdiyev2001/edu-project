package uz.eduplatform.modules.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.content.dto.PublicSubjectDto;
import uz.eduplatform.modules.content.repository.SubjectRepository;

import java.util.List;
import java.util.Map;

/**
 * Publicly accessible endpoints — no authentication required.
 * Used by the landing page to display platform statistics and subjects.
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Publicly accessible platform endpoints")
public class PublicController {

    private final UserRepository userRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final SubjectRepository subjectRepository;

    @GetMapping("/stats")
    @Operation(summary = "Get public platform statistics for landing page")
    @Cacheable(value = "public_stats", key = "'stats'")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        long totalUsers = userRepository.count();
        long totalAttempts = testAttemptRepository.count();
        long totalSubjects = subjectRepository.count();

        Map<String, Long> stats = Map.of(
                "totalUsers", totalUsers,
                "totalAttempts", totalAttempts,
                "totalSubjects", totalSubjects
        );
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/subjects")
    @Operation(summary = "Get localized subjects with metadata for landing page preview")
    @Cacheable(value = "public_stats", key = "'subjects_' + #lang")
    public ResponseEntity<ApiResponse<List<PublicSubjectDto>>> getSubjects(
            @RequestParam(defaultValue = "uzl") String lang) {

        String localeKey = toLocaleKey(lang);

        List<PublicSubjectDto> subjects = subjectRepository
                .findByIsArchivedFalseOrderByQuestionCountDesc(PageRequest.of(0, 24))
                .stream()
                .filter(s -> s.getName() != null && !getLocaleValue(s.getName(), localeKey).isBlank())
                .map(s -> PublicSubjectDto.builder()
                        .name(getLocaleValue(s.getName(), localeKey))
                        .description(getLocaleValue(s.getDescription(), localeKey))
                        .testCount(s.getTestCount())
                        .icon(s.getIcon())
                        .color(s.getColor())
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String toLocaleKey(String lang) {
        return switch (lang) {
            case "uzc" -> "uz_cyrl";
            case "ru"  -> "ru";
            case "en"  -> "en";
            default    -> "uz_latn"; // "uzl" and anything else
        };
    }

    /**
     * Returns the value for the given locale key, falling back to uz_latn, then any non-blank value.
     */
    private String getLocaleValue(Map<String, String> map, String localeKey) {
        if (map == null) return "";
        String val = map.get(localeKey);
        if (val != null && !val.isBlank()) return val;
        // Fallback: uz_latn
        String fallback = map.get("uz_latn");
        if (fallback != null && !fallback.isBlank()) return fallback;
        // Last resort: any non-blank
        return map.values().stream()
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse("");
    }
}
