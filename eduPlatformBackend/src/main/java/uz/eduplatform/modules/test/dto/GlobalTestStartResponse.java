package uz.eduplatform.modules.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalTestStartResponse {
    private UUID attemptId;
    private UUID assignmentId;
    private String testTitle;
    private Integer durationMinutes;
    private Integer questionCount;
}
