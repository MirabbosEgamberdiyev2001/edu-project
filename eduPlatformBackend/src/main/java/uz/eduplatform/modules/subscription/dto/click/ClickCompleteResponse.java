package uz.eduplatform.modules.subscription.dto.click;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickCompleteResponse {

    @JsonProperty("click_trans_id")
    private Long clickTransId;

    @JsonProperty("merchant_trans_id")
    private String merchantTransId;

    @JsonProperty("merchant_confirm_id")
    private String merchantConfirmId;

    private Integer error;

    @JsonProperty("error_note")
    private String errorNote;
}
