package uz.eduplatform.modules.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.test.domain.TestCategory;
import uz.eduplatform.modules.test.domain.TestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestHistoryDto {

    private UUID id;
    private UUID userId;
    private String title;
    private Map<String, String> titleTranslations;
    private TestCategory category;
    private UUID subjectId;
    private String subjectName;
    private List<UUID> topicIds;
    private Integer questionCount;
    private Integer variantCount;
    private Map<String, Integer> difficultyDistribution;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private Long randomSeed;
    private Map<String, Object> headerConfig;
    private List<VariantDto> variants;
    private String testPdfUrl;
    private String answerKeyPdfUrl;
    private String combinedPdfUrl;
    private String proofsPdfUrl;
    private Integer downloadCount;
    private LocalDateTime lastDownloadedAt;
    private Boolean isPublic;
    private String publicSlug;
    private Integer publicDurationMinutes;
    private TestStatus status;
    private LocalDateTime createdAt;
}
