package uz.eduplatform.modules.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.subscription.domain.PaymentProvider;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiationResponse {

    private UUID paymentId;
    private String providerOrderId;
    private PaymentProvider provider;
    private BigDecimal amount;
    private String currency;
    private String redirectUrl;
    private Map<String, Object> providerParams;
}
