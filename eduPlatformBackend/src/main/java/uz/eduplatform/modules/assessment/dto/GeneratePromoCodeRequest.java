package uz.eduplatform.modules.assessment.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePromoCodeRequest {

    @Min(value = 1, message = "{promo.validation.max_uses.min}")
    private Integer maxUses;

    private LocalDateTime expiresAt;
}
