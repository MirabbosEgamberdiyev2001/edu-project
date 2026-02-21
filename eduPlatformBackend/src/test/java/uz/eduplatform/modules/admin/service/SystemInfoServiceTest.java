package uz.eduplatform.modules.admin.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.eduplatform.modules.admin.dto.SystemInfoDto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemInfoServiceTest {

    @Mock private HikariDataSource dataSource;
    @Mock private HikariPoolMXBean poolMXBean;

    @InjectMocks
    private SystemInfoService systemInfoService;

    @Test
    void getSystemInfo_returnsJvmMetrics() {
        when(dataSource.getHikariPoolMXBean()).thenReturn(poolMXBean);
        when(poolMXBean.getActiveConnections()).thenReturn(5);
        when(poolMXBean.getIdleConnections()).thenReturn(10);
        when(poolMXBean.getTotalConnections()).thenReturn(15);
        when(dataSource.getMaximumPoolSize()).thenReturn(20);

        SystemInfoDto result = systemInfoService.getSystemInfo();

        assertNotNull(result);
        assertNotNull(result.getJvm());
        assertTrue(result.getJvm().getMaxMemoryMb() > 0);
        assertTrue(result.getJvm().getAvailableProcessors() > 0);
        assertNotNull(result.getJvm().getJavaVersion());
        assertTrue(result.getJvm().getUsedMemoryMb() >= 0);
        assertNotNull(result.getUptime());
        assertNotNull(result.getServerTime());
    }

    @Test
    void getSystemInfo_returnsDatabasePoolInfo() {
        when(dataSource.getHikariPoolMXBean()).thenReturn(poolMXBean);
        when(poolMXBean.getActiveConnections()).thenReturn(3);
        when(poolMXBean.getIdleConnections()).thenReturn(7);
        when(poolMXBean.getTotalConnections()).thenReturn(10);
        when(dataSource.getMaximumPoolSize()).thenReturn(20);

        SystemInfoDto result = systemInfoService.getSystemInfo();

        assertNotNull(result.getDatabase());
        assertEquals(3, result.getDatabase().getActiveConnections());
        assertEquals(7, result.getDatabase().getIdleConnections());
        assertEquals(10, result.getDatabase().getTotalConnections());
        assertEquals(20, result.getDatabase().getMaxPoolSize());
    }
}
