package uz.eduplatform.modules.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicTreeDto {

    private UUID id;
    private UUID subjectId;
    private UUID parentId;
    private UUID userId;
    private Integer gradeLevel;
    private String name;
    private String description;
    private Map<String, String> nameTranslations;
    private Map<String, String> descriptionTranslations;
    private Integer level;
    private Integer questionCount;
    private Integer sortOrder;
    private Boolean isActive;

    @Builder.Default
    private List<TopicTreeDto> children = new ArrayList<>();
}
