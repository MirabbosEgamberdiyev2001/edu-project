package uz.eduplatform.modules.content.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.content.domain.SubjectCategory;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubjectRequest {

    private Map<String, String> name;

    private Map<String, String> description;

    @Size(max = 500, message = "Icon must be at most 500 characters")
    private String icon;

    @Size(max = 7, message = "Color must be at most 7 characters")
    private String color;

    private SubjectCategory category;

    private Integer gradeLevel;

    private Boolean isActive;
}
