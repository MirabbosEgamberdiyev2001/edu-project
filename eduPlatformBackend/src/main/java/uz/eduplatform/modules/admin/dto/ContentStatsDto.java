package uz.eduplatform.modules.admin.dto;

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
public class ContentStatsDto {

    private List<SubjectStatsDto> subjectStats;
    private List<TopTeacherDto> topTeachers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectStatsDto {
        private UUID subjectId;
        private String subjectName;
        private int topicCount;
        private int questionCount;
        private int testCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopTeacherDto {
        private UUID userId;
        private String firstName;
        private String lastName;
        private String email;
        private long questionCount;
        private long subjectCount;
    }
}
