package uz.eduplatform.modules.group.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSearchDto {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}
