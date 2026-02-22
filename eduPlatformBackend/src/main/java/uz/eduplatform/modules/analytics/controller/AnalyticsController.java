package uz.eduplatform.modules.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.analytics.dto.GroupStatisticsDto;
import uz.eduplatform.modules.analytics.dto.StudentAnalyticsDto;
import uz.eduplatform.modules.analytics.dto.TeacherDashboardDto;
import uz.eduplatform.modules.analytics.service.AnalyticsPdfExportService;
import uz.eduplatform.modules.analytics.service.AnalyticsService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analitika", description = "Analitika va statistika API'lari — o'qituvchi dashboardi, o'quvchi natijalari, guruh statistikasi")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AnalyticsPdfExportService pdfExportService;

    @GetMapping("/teacher/dashboard")
    @Operation(summary = "O'qituvchi dashboardi", description = "O'qituvchining umumiy statistikasi — yaratilgan testlar, o'quvchilar soni, o'rtacha natijalar, eng faol o'quvchilar.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TeacherDashboardDto>> getTeacherDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {

        TeacherDashboardDto dashboard = analyticsService.getTeacherDashboard(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @GetMapping("/teacher/dashboard/export")
    @Operation(summary = "O'qituvchi dashboardini PDF ga eksport qilish", description = "O'qituvchi statistikasini PDF hujjat sifatida yuklab olish.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<byte[]> exportTeacherDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {

        TeacherDashboardDto dashboard = analyticsService.getTeacherDashboard(principal.getId());
        byte[] pdf = pdfExportService.exportTeacherDashboard(dashboard);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=teacher-dashboard.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/student/me")
    @Operation(summary = "O'quvchi shaxsiy analitikasi", description = "Joriy o'quvchining shaxsiy natijalari — fanlar bo'yicha ball, kuchli/zaif tomonlar, o'sish dinamikasi.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentAnalyticsDto>> getMyAnalytics(
            @AuthenticationPrincipal UserPrincipal principal) {

        StudentAnalyticsDto analytics = analyticsService.getStudentAnalytics(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "O'quvchi analitikasi (o'qituvchi ko'rinishi)", description = "O'qituvchi yoki admin tomonidan istalgan o'quvchining analitikasini ko'rish.")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudentAnalyticsDto>> getStudentAnalytics(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        StudentAnalyticsDto analytics = analyticsService.getStudentAnalytics(studentId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Guruh statistikasi", description = "Guruh bo'yicha umumiy statistika — o'rtacha ball, eng yaxshi/yomon natijalar, fanlar bo'yicha taqsimot.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GroupStatisticsDto>> getGroupStatistics(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID groupId) {

        GroupStatisticsDto stats = analyticsService.getGroupStatistics(groupId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/group/{groupId}/export")
    @Operation(summary = "Guruh statistikasini PDF ga eksport qilish", description = "Guruh statistikasini PDF hujjat sifatida yuklab olish.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<byte[]> exportGroupStatistics(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID groupId) {

        GroupStatisticsDto stats = analyticsService.getGroupStatistics(groupId, principal.getId());
        byte[] pdf = pdfExportService.exportGroupStatistics(stats);

        String filename = "group-" + stats.getGroupName().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
