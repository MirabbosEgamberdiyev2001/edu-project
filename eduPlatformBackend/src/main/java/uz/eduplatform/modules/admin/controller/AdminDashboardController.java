package uz.eduplatform.modules.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.modules.admin.dto.ContentStatsDto;
import uz.eduplatform.modules.admin.dto.DashboardStatsDto;
import uz.eduplatform.modules.admin.dto.SystemInfoDto;
import uz.eduplatform.modules.admin.dto.TrendDataDto;
import uz.eduplatform.modules.admin.service.AdminDashboardService;
import uz.eduplatform.modules.admin.service.ContentStatsService;
import uz.eduplatform.modules.admin.service.SystemInfoService;
import uz.eduplatform.modules.admin.service.TrendDataService;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Admin dashboard", description = "Admin boshqaruv paneli API'lari — umumiy statistika, trendlar, tizim ma'lumotlari")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final TrendDataService trendDataService;
    private final ContentStatsService contentStatsService;
    private final SystemInfoService systemInfoService;

    @GetMapping("/stats")
    @Operation(summary = "Dashboard statistikasi", description = "Platformaning umumiy statistikasi — foydalanuvchilar, fanlar, savollar, testlar, obunalar soni.")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        DashboardStatsDto stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/trends")
    @Operation(summary = "Trend ma'lumotlari", description = "Grafik uchun trend ma'lumotlari — haftalik ro'yxatdan o'tish, test yaratilishi, kunlik faol foydalanuvchilar.")
    public ResponseEntity<ApiResponse<TrendDataDto>> getTrendData() {
        TrendDataDto trends = trendDataService.getTrendData();
        return ResponseEntity.ok(ApiResponse.success(trends));
    }

    @GetMapping("/content-stats")
    @Operation(summary = "Kontent statistikasi", description = "Fan bo'yicha savollar taqsimoti, eng faol o'qituvchilar reytingi.")
    public ResponseEntity<ApiResponse<ContentStatsDto>> getContentStats() {
        ContentStatsDto stats = contentStatsService.getContentStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/system-info")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Tizim ma'lumotlari", description = "JVM xotira, ma'lumotlar bazasi pool holati, server versiyasi. Faqat SUPER_ADMIN uchun.")
    public ResponseEntity<ApiResponse<SystemInfoDto>> getSystemInfo() {
        SystemInfoDto info = systemInfoService.getSystemInfo();
        return ResponseEntity.ok(ApiResponse.success(info));
    }
}
