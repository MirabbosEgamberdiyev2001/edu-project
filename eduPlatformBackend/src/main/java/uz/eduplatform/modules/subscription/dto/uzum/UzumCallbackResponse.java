package uz.eduplatform.modules.subscription.dto.uzum;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UzumCallbackResponse {

    private Integer status; // 0 = success, other = error
    private String transactionId;
    private Long confirmTime;
    private String errorMessage;

    public static UzumCallbackResponse success(String transactionId) {
        return UzumCallbackResponse.builder()
                .status(0)
                .transactionId(transactionId)
                .build();
    }

    public static UzumCallbackResponse success(String transactionId, Long confirmTime) {
        return UzumCallbackResponse.builder()
                .status(0)
                .transactionId(transactionId)
                .confirmTime(confirmTime)
                .build();
    }

    public static UzumCallbackResponse error(int status, String errorMessage) {
        return UzumCallbackResponse.builder()
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }
}
