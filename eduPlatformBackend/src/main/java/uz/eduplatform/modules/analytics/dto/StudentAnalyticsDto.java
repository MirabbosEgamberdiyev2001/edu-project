package uz.eduplatform.modules.analytics.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnalyticsDto {

    private UUID studentId;
    private String studentName;    // legacy combined name
    private String firstName;
    private String lastName;

    // Summary — frontend uses overallAverage, totalAttempts, completionRate
    private int totalAssignments;
    private int completedAssignments;
    private int pendingAssignments;
    private BigDecimal overallAverageScore;
    private double overallAverage;     // = overallAverageScore.doubleValue()
    private int totalAttempts;         // = completedAssignments
    private double completionRate;     // = completedAssignments / totalAssignments * 100

    // Score trend — new frontend shape: [{date, value}]
    // (old shape scoreTrendDetails kept for backward compat)
    private List<TrendPointDto> scoreTrend;
    private List<ScoreTrendDetailDto> scoreTrendDetails;

    // Subject breakdown — frontend uses subjectId + totalAttempts
    private List<SubjectBreakdownDto> subjectBreakdown;

    // Weak / strong areas — frontend uses topicId, topicName, subjectName
    private List<WeakAreaDto> weakAreas;
    private List<WeakAreaDto> strongAreas;

    // Weekly activity — frontend expects List<{date, attemptCount}>
    private List<WeeklyActivityItemDto> weeklyActivity;
    private WeeklyActivitySummaryDto weeklyActivitySummary;  // legacy

    // Upcoming / in-progress (not shown in current pages but keep for completeness)
    private List<UpcomingAssignmentDto> upcomingAssignments;
    private List<InProgressAttemptDto> inProgressAttempts;

    // Time management
    private TimeManagementDto timeManagement;

    // ── New inner classes (frontend-compatible) ──

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPointDto {
        private String date;    // ISO date string
        private double value;   // percentage score
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyActivityItemDto {
        private String date;          // ISO date string (YYYY-MM-DD)
        private int attemptCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeakAreaDto {
        private String topicId;       // uses subjectId as proxy
        private String topicName;     // uses subjectName as proxy
        private String subjectName;
        private double averageScore;
        private int attemptCount;
        // Legacy BigDecimal (kept for backward compat)
        private BigDecimal averageScoreDecimal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectBreakdownDto {
        private String subjectId;
        private String subjectName;
        private int totalAttempts;      // frontend name
        private int attemptCount;       // legacy name
        private BigDecimal averageScore;
    }

    // ── Legacy inner classes ──

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreTrendDetailDto {
        private UUID attemptId;
        private String assignmentTitle;
        private BigDecimal percentage;
        private LocalDateTime submittedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingAssignmentDto {
        private UUID assignmentId;
        private String title;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer durationMinutes;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InProgressAttemptDto {
        private UUID attemptId;
        private UUID assignmentId;
        private String assignmentTitle;
        private LocalDateTime startedAt;
        private Long remainingSeconds;
        private Integer answeredQuestions;
        private Integer totalQuestions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyActivitySummaryDto {
        private int testsCompletedThisWeek;
        private int testsCompletedLastWeek;
        private BigDecimal averageScoreThisWeek;
        private long totalTimeSpentMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeManagementDto {
        private BigDecimal averageTimePerTestMinutes;
        private BigDecimal averageTimePerQuestionSeconds;
    }
}
