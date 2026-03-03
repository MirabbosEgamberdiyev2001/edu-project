package uz.eduplatform.modules.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.content.repository.SubjectRepository;

import java.util.List;
import java.util.Map;

/**
 * Publicly accessible endpoints — no authentication required.
 * Used by the landing page to display platform statistics.
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
    @Operation(summary = "Get list of available subjects (for landing page preview)")
    @Cacheable(value = "public_stats", key = "'subjects'")
    public ResponseEntity<ApiResponse<List<String>>> getSubjectNames() {
        List<String> names = subjectRepository.findAll()
                .stream()
                .map(s -> {
                    Map<String, String> nameMap = s.getName();
                    if (nameMap != null) {
                        String uz = nameMap.get("uz_latn");
                        if (uz != null && !uz.isBlank()) return uz;
                        // Fallback to any non-blank value
                        return nameMap.values().stream()
                                .filter(v -> v != null && !v.isBlank())
                                .findFirst()
                                .orElse("");
                    }
                    return "";
                })
                .filter(name -> !name.isBlank())
                .distinct()
                .sorted()
                .limit(20)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(names));
    }
}
