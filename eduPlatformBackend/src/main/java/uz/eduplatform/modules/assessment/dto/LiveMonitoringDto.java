package uz.eduplatform.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveMonitoringDto {

    private UUID assignmentId;
    private String assignmentTitle;
    private int totalAssigned;
    private int totalStarted;
    private int inProgress;
    private int submitted;
    private List<StudentProgressDto> students;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentProgressDto {
        private UUID studentId;
        private String studentName;
        private UUID attemptId;
        private String status;
        private int answeredQuestions;
        private int totalQuestions;
        private int tabSwitchCount;
        private Long remainingSeconds;
        private double percentage;
    }
}
