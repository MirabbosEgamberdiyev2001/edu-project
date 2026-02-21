package uz.eduplatform.modules.subscription.dto.payme;

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
public class PaymeResponse {

    private Long id;
    private Object result;
    private PaymeError error;

    public static PaymeResponse success(Long id, Object result) {
        return PaymeResponse.builder()
                .id(id)
                .result(result)
                .build();
    }

    public static PaymeResponse error(Long id, PaymeError error) {
        return PaymeResponse.builder()
                .id(id)
                .error(error)
                .build();
    }
}
