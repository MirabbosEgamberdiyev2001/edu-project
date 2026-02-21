package uz.eduplatform.modules.analytics.service;

import org.junit.jupiter.api.Test;
import uz.eduplatform.modules.analytics.dto.GroupStatisticsDto;
import uz.eduplatform.modules.analytics.dto.TeacherDashboardDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsPdfExportServiceTest {

    private final AnalyticsPdfExportService exportService = new AnalyticsPdfExportService();

    @Test
    void exportTeacherDashboard_generatesValidPdf() {
        TeacherDashboardDto dto = TeacherDashboardDto.builder()
                .totalGroups(3)
                .totalStudents(25)
                .totalAssignments(10)
                .activeAssignments(2)
                .overallAverageScore(new BigDecimal("75.50"))
                .topStudents(List.of(
                        TeacherDashboardDto.StudentPerformanceDto.builder()
                                .studentId(UUID.randomUUID())
                                .studentName("Aziz Karimov")
                                .averageScore(new BigDecimal("95.00"))
                                .totalAttempts(10)
                                .completedAttempts(10)
                                .build()
                ))
                .atRiskStudents(List.of(
                        TeacherDashboardDto.StudentPerformanceDto.builder()
                                .studentId(UUID.randomUUID())
                                .studentName("Bobur Toshmatov")
                                .averageScore(new BigDecimal("35.00"))
                                .totalAttempts(5)
                                .completedAttempts(5)
                                .build()
                ))
                .recentAssignments(List.of())
                .topicBreakdown(List.of(
                        TeacherDashboardDto.TopicPerformanceDto.builder()
                                .topicId(UUID.randomUUID())
                                .topicName("Algebraik ifodalar")
                                .subjectName("Matematika")
                                .averageScore(new BigDecimal("85.00"))
                                .attemptCount(15)
                                .difficulty("EASY")
                                .build()
                ))
                .build();

        byte[] pdf = exportService.exportTeacherDashboard(dto);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
        // PDF magic bytes: %PDF
        assertThat(pdf[0]).isEqualTo((byte) '%');
        assertThat(pdf[1]).isEqualTo((byte) 'P');
        assertThat(pdf[2]).isEqualTo((byte) 'D');
        assertThat(pdf[3]).isEqualTo((byte) 'F');
    }

    @Test
    void exportGroupStatistics_generatesValidPdf() {
        GroupStatisticsDto dto = GroupStatisticsDto.builder()
                .groupId(UUID.randomUUID())
                .groupName("8-A Sinf")
                .totalMembers(25)
                .totalAssignments(10)
                .completedAssignments(8)
                .groupAverageScore(new BigDecimal("72.00"))
                .highestScore(new BigDecimal("98.00"))
                .lowestScore(new BigDecimal("35.00"))
                .completionRate(new BigDecimal("85.00"))
                .studentScores(List.of(
                        GroupStatisticsDto.StudentScoreDto.builder()
                                .studentId(UUID.randomUUID())
                                .studentName("Aziz Karimov")
                                .averageScore(new BigDecimal("95.00"))
                                .totalAttempts(10)
                                .completedAttempts(10)
                                .trend("UP")
                                .build(),
                        GroupStatisticsDto.StudentScoreDto.builder()
                                .studentId(UUID.randomUUID())
                                .studentName("Ali Valiyev")
                                .averageScore(new BigDecimal("68.00"))
                                .totalAttempts(8)
                                .completedAttempts(7)
                                .trend("STABLE")
                                .build()
                ))
                .assignmentBreakdown(List.of())
                .build();

        byte[] pdf = exportService.exportGroupStatistics(dto);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
        assertThat(pdf[0]).isEqualTo((byte) '%');
        assertThat(pdf[1]).isEqualTo((byte) 'P');
    }

    @Test
    void exportTeacherDashboard_emptyData_generatesValidPdf() {
        TeacherDashboardDto dto = TeacherDashboardDto.builder()
                .totalGroups(0)
                .totalStudents(0)
                .totalAssignments(0)
                .activeAssignments(0)
                .overallAverageScore(BigDecimal.ZERO)
                .topStudents(List.of())
                .atRiskStudents(List.of())
                .recentAssignments(List.of())
                .topicBreakdown(List.of())
                .build();

        byte[] pdf = exportService.exportTeacherDashboard(dto);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
    }
}
