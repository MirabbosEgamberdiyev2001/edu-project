package uz.eduplatform.modules.content.dto;

import jakarta.validation.constraints.NotNull;
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
public class CreateSubjectRequest {

    @NotNull(message = "{content.validation.subject.name.required}")
    private Map<String, String> name;

    private Map<String, String> description;

    @Size(max = 500, message = "{content.validation.icon.size}")
    private String icon;

    @Size(max = 7, message = "{content.validation.color.size}")
    private String color;

    private SubjectCategory category;

    private Integer gradeLevel;
}
