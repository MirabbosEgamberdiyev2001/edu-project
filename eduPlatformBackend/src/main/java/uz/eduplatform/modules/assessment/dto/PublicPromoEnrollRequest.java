package uz.eduplatform.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PublicPromoEnrollRequest {

    @NotBlank(message = "{validation.required}")
    @Size(min = 2, max = 50, message = "{validation.size}")
    private String firstName;

    @NotBlank(message = "{validation.required}")
    @Size(min = 2, max = 50, message = "{validation.size}")
    private String lastName;

    @NotBlank(message = "{validation.required}")
    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "{validation.phone.invalid}")
    private String phone;
}
