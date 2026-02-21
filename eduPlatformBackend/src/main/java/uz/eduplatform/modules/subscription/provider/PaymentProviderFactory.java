package uz.eduplatform.modules.subscription.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.modules.subscription.domain.PaymentProvider;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentProviderFactory {

    private final List<PaymentProviderStrategy> strategies;

    public PaymentProviderStrategy getStrategy(PaymentProvider provider) {
        PaymentProviderStrategy strategy = strategies.stream()
                .filter(s -> s.getProvider() == provider)
                .findFirst()
                .orElseThrow(() -> BusinessException.ofKey("payment.provider.unsupported", provider.name()));

        if (!strategy.isEnabled()) {
            throw BusinessException.ofKey("payment.provider.disabled", provider.name());
        }

        return strategy;
    }
}
