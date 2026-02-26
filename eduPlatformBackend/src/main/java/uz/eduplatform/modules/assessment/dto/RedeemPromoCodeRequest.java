package uz.eduplatform.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedeemPromoCodeRequest {

    @NotBlank(message = "{promo.validation.code.required}")
    @Size(min = 8, max = 8, message = "{promo.validation.code.size}")
    private String code;
}
