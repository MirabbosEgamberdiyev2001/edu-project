package uz.eduplatform.modules.admin.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uz.eduplatform.modules.admin.dto.SystemInfoDto;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class SystemInfoService {

    private final DataSource dataSource;

    @Cacheable(value = "system_info", key = "'global'")
    public SystemInfoDto getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);

        SystemInfoDto.JvmInfoDto jvmInfo = SystemInfoDto.JvmInfoDto.builder()
                .maxMemoryMb(maxMemory)
                .totalMemoryMb(totalMemory)
                .freeMemoryMb(freeMemory)
                .usedMemoryMb(totalMemory - freeMemory)
                .availableProcessors(runtime.availableProcessors())
                .javaVersion(System.getProperty("java.version"))
                .build();

        SystemInfoDto.DatabaseInfoDto dbInfo = getDatabaseInfo();

        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration uptime = Duration.ofMillis(uptimeMillis);
        String uptimeStr = String.format("%dd %dh %dm %ds",
                uptime.toDays(), uptime.toHoursPart(), uptime.toMinutesPart(), uptime.toSecondsPart());

        return SystemInfoDto.builder()
                .jvm(jvmInfo)
                .database(dbInfo)
                .uptime(uptimeStr)
                .serverTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private SystemInfoDto.DatabaseInfoDto getDatabaseInfo() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            if (poolMXBean != null) {
                return SystemInfoDto.DatabaseInfoDto.builder()
                        .activeConnections(poolMXBean.getActiveConnections())
                        .idleConnections(poolMXBean.getIdleConnections())
                        .totalConnections(poolMXBean.getTotalConnections())
                        .maxPoolSize(hikariDataSource.getMaximumPoolSize())
                        .build();
            }
        }
        return SystemInfoDto.DatabaseInfoDto.builder()
                .activeConnections(-1)
                .idleConnections(-1)
                .totalConnections(-1)
                .maxPoolSize(-1)
                .build();
    }
}
