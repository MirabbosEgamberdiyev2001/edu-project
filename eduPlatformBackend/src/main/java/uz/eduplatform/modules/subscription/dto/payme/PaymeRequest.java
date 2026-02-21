package uz.eduplatform.modules.subscription.dto.payme;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymeRequest {

    private String method;
    private Map<String, Object> params;
    private Long id;

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccount() {
        return (Map<String, Object>) params.get("account");
    }

    public String getAccountOrderId() {
        Map<String, Object> account = getAccount();
        return account != null ? (String) account.get("order_id") : null;
    }

    public Long getAmount() {
        Object amount = params.get("amount");
        if (amount instanceof Number) {
            return ((Number) amount).longValue();
        }
        return null;
    }

    @JsonProperty("id")
    public String getTransactionId() {
        Object id = params.get("id");
        return id != null ? id.toString() : null;
    }

    public Long getFrom() {
        Object from = params.get("from");
        return from instanceof Number ? ((Number) from).longValue() : null;
    }

    public Long getTo() {
        Object to = params.get("to");
        return to instanceof Number ? ((Number) to).longValue() : null;
    }

    public Integer getReason() {
        Object reason = params.get("reason");
        return reason instanceof Number ? ((Number) reason).intValue() : null;
    }
}
