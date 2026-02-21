package uz.eduplatform.modules.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.content.domain.Difficulty;
import uz.eduplatform.modules.content.domain.QuestionStatus;
import uz.eduplatform.modules.content.domain.QuestionType;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionFilterRequest {

    private UUID subjectId;
    private UUID topicId;
    private QuestionType questionType;
    private Difficulty difficulty;
    private QuestionStatus status;
    private String search;
}
