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

    // Core counts
    private int totalGroups;
    private int totalStudents;
    private int totalAssignments;
    private int activeAssignments;
    private int totalTests;

    // Scores — frontend uses averageScore & completionRate (double)
    private BigDecimal overallAverageScore;
    private double averageScore;
    private double completionRate;

    // Trend data (monthly test creation — {date, value})
    private List<TrendPointDto> testCreationTrend;

    // Recent activity feed
    private List<ActivityDto> recentActivity;

    // Top / at-risk students
    private List<StudentPerformanceDto> topStudents;
    private List<StudentPerformanceDto> atRiskStudents;

    // Legacy fields (still populated for backward compat)
    private List<AssignmentSummaryDto> recentAssignments;
    private List<TopicPerformanceDto> topicBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPointDto {
        private String date;   // e.g. "2024-01"
        private double value;  // count
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityDto {
        private String type;
        private String description;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentPerformanceDto {
        private UUID studentId;
        private String studentName;   // legacy combined
        // Frontend-expected split names
        private String firstName;
        private String lastName;

        private BigDecimal averageScore;
        private int totalAttempts;
        private int completedAttempts;
        private double completionRate;

        // at-risk extras
        private int missedAssignments;
        private String lastActivityAt;
    }

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
        private String difficulty;
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
        private String createdAt;
    }
}
