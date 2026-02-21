package uz.eduplatform.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfoDto {

    private JvmInfoDto jvm;
    private DatabaseInfoDto database;
    private String uptime;
    private String serverTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JvmInfoDto {
        private long maxMemoryMb;
        private long totalMemoryMb;
        private long freeMemoryMb;
        private long usedMemoryMb;
        private int availableProcessors;
        private String javaVersion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseInfoDto {
        private int activeConnections;
        private int idleConnections;
        private int totalConnections;
        private int maxPoolSize;
    }
}
