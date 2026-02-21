package uz.eduplatform.modules.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.subscription.domain.UsageType;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageDto {
    private UUID userId;
    private UsageType usageType;
    private LocalDate date;
    private Integer count;
    private Integer limit;
    private Boolean unlimited;
}
