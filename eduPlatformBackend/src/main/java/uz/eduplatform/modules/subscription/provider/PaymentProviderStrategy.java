package uz.eduplatform.modules.subscription.provider;

import uz.eduplatform.modules.subscription.domain.Payment;
import uz.eduplatform.modules.subscription.domain.PaymentProvider;
import uz.eduplatform.modules.subscription.domain.SubscriptionPlan;
import uz.eduplatform.modules.subscription.dto.PaymentInitiationResponse;

public interface PaymentProviderStrategy {

    PaymentProvider getProvider();

    PaymentInitiationResponse initiateCheckout(Payment payment, SubscriptionPlan plan);

    boolean isEnabled();
}
