package uz.eduplatform.modules.subscription.dto.click;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClickCompleteRequest {

    @JsonProperty("click_trans_id")
    private Long clickTransId;

    @JsonProperty("service_id")
    private Long serviceId;

    @JsonProperty("click_paydoc_id")
    private Long clickPaydocId;

    @JsonProperty("merchant_trans_id")
    private String merchantTransId;

    @JsonProperty("merchant_prepare_id")
    private String merchantPrepareId;

    private Double amount;

    private Integer action;

    private String error;

    @JsonProperty("error_note")
    private String errorNote;

    @JsonProperty("sign_time")
    private String signTime;

    @JsonProperty("sign_string")
    private String signString;
}
