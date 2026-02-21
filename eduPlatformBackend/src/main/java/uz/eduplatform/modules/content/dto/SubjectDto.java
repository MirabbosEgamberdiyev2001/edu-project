package uz.eduplatform.modules.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.eduplatform.modules.content.domain.SubjectCategory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDto {

    private UUID id;
    private UUID userId;
    private String name;
    private String description;
    private Map<String, String> nameTranslations;
    private Map<String, String> descriptionTranslations;
    private String icon;
    private String color;
    private SubjectCategory category;
    private Integer gradeLevel;
    private Boolean isTemplate;
    private UUID templateId;
    private Boolean isActive;
    private Boolean isArchived;
    private Integer topicCount;
    private Integer questionCount;
    private Integer testCount;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
