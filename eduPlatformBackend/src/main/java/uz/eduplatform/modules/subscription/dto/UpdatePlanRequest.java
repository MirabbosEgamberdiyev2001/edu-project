package uz.eduplatform.modules.subscription.dto;

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
public class UpdatePlanRequest {
    private String name;
    private String description;
    private PlanType planType;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private String currency;

    private Integer maxTestsPerDay;
    private Integer maxTestsPerMonth;
    private Integer maxGroups;
    private Integer maxStudentsPerGroup;
    private Integer maxExportsPerMonth;
    private Integer maxQuestionsPerImport;

    private Boolean analyticsEnabled;
    private Boolean exportPdfEnabled;
    private Boolean exportDocxEnabled;
    private Boolean proofVisible;
    private Boolean apiAccess;
    private Boolean customBranding;

    private Boolean active;
    private Integer sortOrder;
}
