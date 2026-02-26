package uz.eduplatform.modules.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoCodeDto {
    private UUID id;
    private UUID assignmentId;
    private String code;
    private Integer maxUses;
    private Integer currentUses;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
