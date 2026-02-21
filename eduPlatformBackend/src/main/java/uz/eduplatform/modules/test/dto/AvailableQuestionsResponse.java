package uz.eduplatform.modules.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableQuestionsResponse {

    private int totalAvailable;
    private int easyCount;
    private int mediumCount;
    private int hardCount;
    private int maxPossibleQuestions;
}
