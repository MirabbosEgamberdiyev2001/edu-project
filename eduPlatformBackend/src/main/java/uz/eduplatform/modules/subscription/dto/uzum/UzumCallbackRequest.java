package uz.eduplatform.modules.subscription.dto.uzum;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UzumCallbackRequest {

    @JsonProperty("serviceId")
    private String serviceId;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("merchantTransId")
    private String merchantTransId;

    private Long amount;

    private String method; // check, create, confirm, reverse

    private Long timestamp;

    private Map<String, Object> params;
}
