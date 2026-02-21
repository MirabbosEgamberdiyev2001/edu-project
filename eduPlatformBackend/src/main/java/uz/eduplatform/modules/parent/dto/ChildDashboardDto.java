package uz.eduplatform.modules.parent.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDashboardDto {

    private UUID childId;
    private String childName;

    private int totalAssignments;
    private int completedAssignments;
    private int pendingAssignments;

    private BigDecimal averageScore;
    private String scoreTrend; // UP, DOWN, STABLE

    private List<RecentAttemptDto> recentAttempts;
    private List<SubjectScoreDto> subjectBreakdown;
    private WeeklyActivityDto weeklyActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentAttemptDto {
        private UUID attemptId;
        private String assignmentTitle;
        private BigDecimal percentage;
        private String status;
        private LocalDateTime submittedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectScoreDto {
        private String subjectName;
        private BigDecimal averageScore;
        private int attemptCount;
        private String level; // EXCELLENT, GOOD, ATTENTION, CRITICAL
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyActivityDto {
        private int testsCompletedToday;
        private int testsCompletedThisWeek;
        private int testsCompletedThisMonth;
        private BigDecimal averageScoreThisWeek;
        private long totalTimeSpentMinutesToday;
    }
}
