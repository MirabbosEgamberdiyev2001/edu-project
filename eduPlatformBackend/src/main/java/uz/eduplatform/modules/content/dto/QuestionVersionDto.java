package uz.eduplatform.modules.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVersionDto {

    private UUID id;
    private UUID questionId;
    private Integer version;
    private String questionText;
    private Map<String, String> questionTextTranslations;
    private Object options;
    private Object correctAnswer;
    private String proof;
    private Map<String, String> proofTranslations;
    private UUID changedBy;
    private String changeReason;
    private LocalDateTime createdAt;
}
