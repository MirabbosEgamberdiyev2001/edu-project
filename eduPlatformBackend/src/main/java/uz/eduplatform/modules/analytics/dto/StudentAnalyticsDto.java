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
    private String studentName;

    // Summary
    private int totalAssignments;
    private int completedAssignments;
    private int pendingAssignments;
    private BigDecimal overallAverageScore;

    // Score trend (last N attempts)
    private List<ScoreTrendDto> scoreTrend;

    // Per-subject breakdown
    private List<SubjectBreakdownDto> subjectBreakdown;

    // Upcoming assignments
    private List<UpcomingAssignmentDto> upcomingAssignments;

    // In-progress attempts
    private List<InProgressAttemptDto> inProgressAttempts;

    // Weekly activity
    private WeeklyActivityDto weeklyActivity;

    // Weak areas
    private List<WeakAreaDto> weakAreas;

    // Time management
    private TimeManagementDto timeManagement;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreTrendDto {
        private UUID attemptId;
        private String assignmentTitle;
        private BigDecimal percentage;
        private LocalDateTime submittedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectBreakdownDto {
        private String subjectName;
        private int attemptCount;
        private BigDecimal averageScore;
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
    public static class WeeklyActivityDto {
        private int testsCompletedThisWeek;
        private int testsCompletedLastWeek;
        private BigDecimal averageScoreThisWeek;
        private long totalTimeSpentMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeakAreaDto {
        private String subjectName;
        private BigDecimal averageScore;
        private int attemptCount;
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
