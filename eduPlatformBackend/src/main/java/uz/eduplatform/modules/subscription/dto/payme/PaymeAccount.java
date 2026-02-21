package uz.eduplatform.modules.subscription.dto.payme;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymeAccount {

    @JsonProperty("order_id")
    private String orderId;
}
