package uz.eduplatform.modules.parent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairWithCodeRequest {

    @NotBlank(message = "{parent.validation.pairing_code.required}")
    @Size(min = 8, max = 8, message = "{parent.validation.pairing_code.size}")
    private String code;
}
