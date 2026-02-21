package uz.eduplatform.modules.admin.dto;

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
public class DashboardStatsDto {

    private long totalUsers;
    private long activeUsers;
    private long newUsersThisWeek;
    private long totalSubjects;
    private long totalTopics;
    private long totalQuestions;
    private long pendingQuestions;
    private long activeQuestions;
    private long totalTests;
    private long testsThisWeek;
    private long activeSessionsToday;
    private long totalDownloads;

    private UsersByRoleDto usersByRole;
    private UsersByStatusDto usersByStatus;
    private QuestionsByDifficultyDto questionsByDifficulty;
    private QuestionsByTypeDto questionsByType;
    private NotificationStatsDto notificationStats;
    private List<RecentActivityDto> recentActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsersByRoleDto {
        private long superAdmins;
        private long admins;
        private long moderators;
        private long teachers;
        private long parents;
        private long students;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsersByStatusDto {
        private long active;
        private long inactive;
        private long blocked;
        private long pendingVerification;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionsByDifficultyDto {
        private long easy;
        private long medium;
        private long hard;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionsByTypeDto {
        private long mcqSingle;
        private long mcqMulti;
        private long trueFalse;
        private long fillBlank;
        private long matching;
        private long ordering;
        private long shortAnswer;
        private long essay;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationStatsDto {
        private long totalSent;
        private long totalFailed;
        private long totalPending;
        private long totalRetrying;
        private long bySms;
        private long byEmail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityDto {
        private UUID id;
        private UUID userId;
        private String action;
        private String actionCategory;
        private String entityType;
        private LocalDateTime createdAt;
    }
}
