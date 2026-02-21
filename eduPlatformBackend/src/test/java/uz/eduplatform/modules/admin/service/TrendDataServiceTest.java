package uz.eduplatform.modules.admin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.eduplatform.modules.admin.dto.TrendDataDto;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.auth.repository.UserSessionRepository;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendDataServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TestHistoryRepository testHistoryRepository;
    @Mock private UserSessionRepository userSessionRepository;

    @InjectMocks
    private TrendDataService trendDataService;

    @Test
    void getTrendData_returnsWeeklyRegistrations() {
        LocalDate weekStart = LocalDate.of(2026, 1, 5);
        Object[] row = new Object[]{Date.valueOf(weekStart), 15L};

        List<Object[]> registrations = new ArrayList<>();
        registrations.add(row);
        when(userRepository.countWeeklyRegistrations(any(LocalDateTime.class)))
                .thenReturn(registrations);
        when(testHistoryRepository.countWeeklyTestCreations(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(userSessionRepository.countDailyActiveUsers(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        TrendDataDto result = trendDataService.getTrendData();

        assertNotNull(result);
        assertEquals(1, result.getWeeklyRegistrations().size());
        assertEquals(weekStart, result.getWeeklyRegistrations().get(0).getDate());
        assertEquals(15L, result.getWeeklyRegistrations().get(0).getCount());
    }

    @Test
    void getTrendData_returnsEmptyListsWhenNoData() {
        when(userRepository.countWeeklyRegistrations(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(testHistoryRepository.countWeeklyTestCreations(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(userSessionRepository.countDailyActiveUsers(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        TrendDataDto result = trendDataService.getTrendData();

        assertNotNull(result);
        assertTrue(result.getWeeklyRegistrations().isEmpty());
        assertTrue(result.getWeeklyTestCreations().isEmpty());
        assertTrue(result.getDailyActiveUsers().isEmpty());
    }
}
