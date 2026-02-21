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
    private int totalMembers;
    private int totalAssignments;
    private int completedAssignments;

    private BigDecimal groupAverageScore;
    private BigDecimal highestScore;
    private BigDecimal lowestScore;
    private BigDecimal completionRate;

    private List<StudentScoreDto> studentScores;
    private List<AssignmentBreakdownDto> assignmentBreakdown;

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
        private String trend; // UP, DOWN, STABLE
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
}
