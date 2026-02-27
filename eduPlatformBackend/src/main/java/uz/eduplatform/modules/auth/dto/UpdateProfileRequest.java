package uz.eduplatform.modules.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 100, message = "{auth.validation.firstname.size}")
    private String firstName;

    @Size(max = 100, message = "{auth.validation.lastname.size}")
    private String lastName;

    private String avatarUrl;
    private String locale;
    private String timezone;

    @Size(max = 1000)
    private String bio;

    @Size(max = 255)
    private String workplace;

    private UUID subjectId;
}
