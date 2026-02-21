package uz.eduplatform.modules.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import uz.eduplatform.modules.analytics.dto.GroupStatisticsDto;
import uz.eduplatform.modules.analytics.dto.TeacherDashboardDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsPdfExportService {

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 18;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public byte[] exportTeacherDashboard(TeacherDashboardDto dto) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font fontBold = PDType1Font.HELVETICA_BOLD;
            PDType1Font fontNormal = PDType1Font.HELVETICA;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = page.getMediaBox().getHeight() - MARGIN;

                // Title
                y = drawText(cs, "Teacher Dashboard Report", fontBold, 16, MARGIN, y);
                y = drawText(cs, "Date: " + LocalDate.now().format(DATE_FMT), fontNormal, 10, MARGIN, y);
                y -= LINE_HEIGHT;

                // Summary
                y = drawText(cs, "Summary", fontBold, 13, MARGIN, y);
                y = drawText(cs, "Total Groups: " + dto.getTotalGroups(), fontNormal, 11, MARGIN + 10, y);
                y = drawText(cs, "Total Students: " + dto.getTotalStudents(), fontNormal, 11, MARGIN + 10, y);
                y = drawText(cs, "Total Assignments: " + dto.getTotalAssignments(), fontNormal, 11, MARGIN + 10, y);
                y = drawText(cs, "Active Assignments: " + dto.getActiveAssignments(), fontNormal, 11, MARGIN + 10, y);
                y = drawText(cs, "Overall Average Score: " + formatScore(dto.getOverallAverageScore()) + "%",
                        fontNormal, 11, MARGIN + 10, y);
                y -= LINE_HEIGHT;

                // Top Students
                if (dto.getTopStudents() != null && !dto.getTopStudents().isEmpty()) {
                    y = drawText(cs, "Top Students", fontBold, 13, MARGIN, y);
                    for (TeacherDashboardDto.StudentPerformanceDto s : dto.getTopStudents()) {
                        y = drawText(cs, "  " + s.getStudentName() + " - " + formatScore(s.getAverageScore()) + "% "
                                        + "(" + s.getCompletedAttempts() + " attempts)",
                                fontNormal, 10, MARGIN + 10, y);
                    }
                    y -= LINE_HEIGHT;
                }

                // At-risk Students
                if (dto.getAtRiskStudents() != null && !dto.getAtRiskStudents().isEmpty()) {
                    y = drawText(cs, "At-Risk Students (below 40%)", fontBold, 13, MARGIN, y);
                    for (TeacherDashboardDto.StudentPerformanceDto s : dto.getAtRiskStudents()) {
                        y = drawText(cs, "  ! " + s.getStudentName() + " - " + formatScore(s.getAverageScore()) + "%",
                                fontNormal, 10, MARGIN + 10, y);
                    }
                    y -= LINE_HEIGHT;
                }

                // Topic breakdown
                if (dto.getTopicBreakdown() != null && !dto.getTopicBreakdown().isEmpty()) {
                    y = drawText(cs, "Topic Performance", fontBold, 13, MARGIN, y);
                    for (TeacherDashboardDto.TopicPerformanceDto t : dto.getTopicBreakdown()) {
                        String label = t.getTopicName() + " (" + t.getSubjectName() + ")";
                        y = drawText(cs, "  " + label + " - " + formatScore(t.getAverageScore()) + "% ["
                                        + t.getDifficulty() + "]",
                                fontNormal, 10, MARGIN + 10, y);
                        if (y < MARGIN + 20) {
                            // New page if running out of space
                            cs.close();
                            PDPage newPage = new PDPage(PDRectangle.A4);
                            doc.addPage(newPage);
                            // We cannot easily reopen content stream in this simple approach
                            break;
                        }
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate teacher dashboard PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    public byte[] exportGroupStatistics(GroupStatisticsDto dto) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font fontBold = PDType1Font.HELVETICA_BOLD;
            PDType1Font fontNormal = PDType1Font.HELVETICA;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = page.getMediaBox().getHeight() - MARGIN;

                // Title
                y = drawText(cs, "Group Report: " + dto.getGroupName(), fontBold, 16, MARGIN, y);
                y = drawText(cs, "Date: " + LocalDate.now().format(DATE_FMT), fontNormal, 10, MARGIN, y);
                y -= LINE_HEIGHT;

                // Summary
                y = drawText(cs, "Summary", fontBold, 13, MARGIN, y);
                y = drawText(cs, "Total Members: " + dto.getTotalMembers(), fontNormal, 11, MARGIN + 10, y);
                y = drawText(cs, "Total Assignments: " + dto.getTotalAssignments(), fontNormal, 11, MARGIN + 10, y);
                y = drawText(cs, "Group Average: " + formatScore(dto.getGroupAverageScore()) + "%",
                        fontNormal, 11, MARGIN + 10, y);
                y = drawText(cs, "Highest Score: " + formatScore(dto.getHighestScore()) + "%",
                        fontNormal, 11, MARGIN + 10, y);
                y = drawText(cs, "Lowest Score: " + formatScore(dto.getLowestScore()) + "%",
                        fontNormal, 11, MARGIN + 10, y);
                y = drawText(cs, "Completion Rate: " + formatScore(dto.getCompletionRate()) + "%",
                        fontNormal, 11, MARGIN + 10, y);
                y -= LINE_HEIGHT;

                // Student scores
                if (dto.getStudentScores() != null && !dto.getStudentScores().isEmpty()) {
                    y = drawText(cs, "Student Scores", fontBold, 13, MARGIN, y);
                    int rank = 1;
                    for (GroupStatisticsDto.StudentScoreDto s : dto.getStudentScores()) {
                        String trendArrow = "UP".equals(s.getTrend()) ? " ^" :
                                "DOWN".equals(s.getTrend()) ? " v" : " =";
                        y = drawText(cs, "  " + rank + ". " + s.getStudentName()
                                        + " - " + formatScore(s.getAverageScore()) + "%" + trendArrow
                                        + " (" + s.getCompletedAttempts() + "/" + s.getTotalAttempts() + " attempts)",
                                fontNormal, 10, MARGIN + 10, y);
                        rank++;
                        if (y < MARGIN + 20) break;
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate group statistics PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private float drawText(PDPageContentStream cs, String text, PDType1Font font, float fontSize,
                            float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - LINE_HEIGHT;
    }

    private String formatScore(BigDecimal score) {
        return score != null ? score.toPlainString() : "0";
    }
}
