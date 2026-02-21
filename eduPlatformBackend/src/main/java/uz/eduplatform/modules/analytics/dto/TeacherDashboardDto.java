package uz.eduplatform.modules.analytics.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardDto {

    // Summary stats
    private int totalGroups;
    private int totalStudents;
    private int totalAssignments;
    private int activeAssignments;

    // Performance overview
    private BigDecimal overallAverageScore;

    // Top performing students
    private List<StudentPerformanceDto> topStudents;

    // At-risk students (low average)
    private List<StudentPerformanceDto> atRiskStudents;

    // Recent assignment results
    private List<AssignmentSummaryDto> recentAssignments;

    // Topic-level analysis
    private List<TopicPerformanceDto> topicBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicPerformanceDto {
        private UUID topicId;
        private String topicName;
        private String subjectName;
        private BigDecimal averageScore;
        private int attemptCount;
        private String difficulty; // EASY, MEDIUM, HARD
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentPerformanceDto {
        private UUID studentId;
        private String studentName;
        private BigDecimal averageScore;
        private int totalAttempts;
        private int completedAttempts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentSummaryDto {
        private UUID assignmentId;
        private String title;
        private int totalStudents;
        private int submittedCount;
        private BigDecimal averageScore;
        private String status;
    }
}
