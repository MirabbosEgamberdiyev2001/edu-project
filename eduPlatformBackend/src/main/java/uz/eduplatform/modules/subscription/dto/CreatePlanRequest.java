package uz.eduplatform.modules.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.subscription.domain.PlanType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanRequest {

    @NotBlank(message = "{subscription.validation.plan.name.required}")
    private String name;

    private String description;

    @NotNull(message = "{subscription.validation.plan.type.required}")
    private PlanType planType;

    @NotNull(message = "{subscription.validation.plan.price.required}")
    private BigDecimal priceMonthly;

    private BigDecimal priceYearly;

    private String currency;

    // Usage limits
    private Integer maxTestsPerDay;
    private Integer maxTestsPerMonth;
    private Integer maxGroups;
    private Integer maxStudentsPerGroup;
    private Integer maxExportsPerMonth;
    private Integer maxQuestionsPerImport;

    // Feature flags
    private Boolean analyticsEnabled;
    private Boolean exportPdfEnabled;
    private Boolean exportDocxEnabled;
    private Boolean proofVisible;
    private Boolean apiAccess;
    private Boolean customBranding;

    private Integer sortOrder;
}
