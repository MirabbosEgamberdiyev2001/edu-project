package uz.eduplatform.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveMonitoringDto {

    private UUID assignmentId;
    private int totalStudents;
    private int activeStudents;
    private int completedStudents;
    private int notStartedStudents;
    private List<LiveStudentDto> students;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveStudentDto {
        private UUID studentId;
        private String firstName;
        private String lastName;
        private String status;
        private Integer currentQuestion;
        private int totalQuestions;
        private int answeredQuestions;
        private int tabSwitches;
        private LocalDateTime startedAt;
        private Long timeRemaining;
    }
}
