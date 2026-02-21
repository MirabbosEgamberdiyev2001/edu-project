package uz.eduplatform.modules.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignSubscriptionRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID planId;

    private Integer durationMonths;

    private Boolean autoRenew;
}
