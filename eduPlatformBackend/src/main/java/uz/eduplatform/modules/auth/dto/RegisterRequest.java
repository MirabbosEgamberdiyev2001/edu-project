package uz.eduplatform.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.auth.domain.Role;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @Email(message = "{auth.validation.email.invalid}")
    private String email;

    @Pattern(regexp = "^\\+998[0-9]{9}$", message = "{auth.validation.phone.invalid}")
    private String phone;

    @NotBlank(message = "{auth.validation.password.required}")
    @Size(min = 6, max = 64, message = "{auth.validation.password.size}")
    private String password;

    @NotBlank(message = "{auth.validation.firstname.required}")
    @Size(max = 100, message = "{auth.validation.firstname.size}")
    private String firstName;

    @NotBlank(message = "{auth.validation.lastname.required}")
    @Size(max = 100, message = "{auth.validation.lastname.size}")
    private String lastName;

    private Role role;
}
