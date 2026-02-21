package uz.eduplatform.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataDto {

    private List<TrendPoint> weeklyRegistrations;
    private List<TrendPoint> weeklyTestCreations;
    private List<TrendPoint> dailyActiveUsers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private LocalDate date;
        private long count;
    }
}
