package uz.eduplatform.modules.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.modules.admin.dto.TrendDataDto;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.auth.repository.UserSessionRepository;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendDataService {

    private final UserRepository userRepository;
    private final TestHistoryRepository testHistoryRepository;
    private final UserSessionRepository userSessionRepository;

    @Transactional(readOnly = true)
    public TrendDataDto getTrendData() {
        LocalDateTime twelveWeeksAgo = LocalDateTime.now().minusWeeks(12);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        return TrendDataDto.builder()
                .weeklyRegistrations(mapToTrendPoints(userRepository.countWeeklyRegistrations(twelveWeeksAgo)))
                .weeklyTestCreations(mapToTrendPoints(testHistoryRepository.countWeeklyTestCreations(twelveWeeksAgo)))
                .dailyActiveUsers(mapToTrendPoints(userSessionRepository.countDailyActiveUsers(thirtyDaysAgo)))
                .build();
    }

    private List<TrendDataDto.TrendPoint> mapToTrendPoints(List<Object[]> results) {
        List<TrendDataDto.TrendPoint> points = new ArrayList<>();
        for (Object[] row : results) {
            LocalDate date;
            if (row[0] instanceof Date sqlDate) {
                date = sqlDate.toLocalDate();
            } else {
                date = ((java.sql.Timestamp) row[0]).toLocalDateTime().toLocalDate();
            }
            long count = ((Number) row[1]).longValue();
            points.add(TrendDataDto.TrendPoint.builder()
                    .date(date)
                    .count(count)
                    .build());
        }
        return points;
    }
}
