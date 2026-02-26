package uz.eduplatform.modules.analytics.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupStatisticsDto {

    private UUID groupId;
    private String groupName;

    // Legacy fields
    private int totalMembers;
    private int totalAssignments;
    private int completedAssignments;
    private BigDecimal groupAverageScore;
    private BigDecimal highestScore;
    private BigDecimal lowestScore;
    private BigDecimal completionRate;
    private List<StudentScoreDto> studentScores;
    private List<AssignmentBreakdownDto> assignmentBreakdown;

    // Frontend-compatible fields
    private int memberCount;            // = totalMembers
    private double averageScore;        // = groupAverageScore.doubleValue()
    private List<ScoreDistributionDto> scoreDistribution;
    private List<StudentRankingDto> studentRankings;
    private List<SubjectPerformanceDto> subjectPerformance;

    // ── Legacy inner classes ──

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentScoreDto {
        private UUID studentId;
        private String studentName;
        private BigDecimal averageScore;
        private int totalAttempts;
        private int completedAttempts;
        private String trend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentBreakdownDto {
        private UUID assignmentId;
        private String title;
        private BigDecimal averageScore;
        private int submittedCount;
        private int totalAssigned;
        private String status;
    }

    // ── Frontend-compatible inner classes ──

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDistributionDto {
        private String range;   // e.g. "0-40", "40-60", "60-80", "80-100"
        private int count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentRankingDto {
        private int rank;
        private UUID studentId;
        private String firstName;
        private String lastName;
        private double averageScore;
        private int attemptCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectPerformanceDto {
        private UUID subjectId;
        private String subjectName;
        private double averageScore;
        private int assignmentCount;
    }
}
