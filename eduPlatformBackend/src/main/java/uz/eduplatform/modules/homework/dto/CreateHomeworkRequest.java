package uz.eduplatform.modules.homework.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateHomeworkRequest {

    @NotBlank
    private String title;

    private String description;
    private UUID groupId;
    private LocalDateTime deadline;
    private Boolean allowResubmission = true;
    private Boolean fileRequired = false;
    private Integer maxFileSizeMb = 10;
}
