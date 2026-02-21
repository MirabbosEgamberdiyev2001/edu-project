package uz.eduplatform.modules.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.content.domain.Difficulty;
import uz.eduplatform.modules.content.domain.QuestionStatus;
import uz.eduplatform.modules.content.domain.QuestionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {

    private UUID id;
    private UUID topicId;
    private String topicName;
    private Map<String, String> topicNameTranslations;
    private UUID subjectId;
    private String subjectName;
    private Map<String, String> subjectNameTranslations;
    private UUID userId;
    private String userName;

    private String questionText;
    private Map<String, String> questionTextTranslations;
    private QuestionType questionType;
    private Difficulty difficulty;
    private BigDecimal points;
    private Integer timeLimitSeconds;

    private Map<String, Object> media;
    private Object options;
    private Object correctAnswer;

    private String proof;
    private Map<String, String> proofTranslations;
    private Boolean proofRequired;

    private QuestionStatus status;
    private String rejectionReason;
    private UUID moderatedBy;
    private LocalDateTime moderatedAt;

    private Integer timesUsed;
    private BigDecimal correctRate;
    private Integer avgTimeSeconds;

    private Integer version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
