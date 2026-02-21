package uz.eduplatform.modules.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicDto {

    private UUID id;
    private UUID subjectId;
    private UUID parentId;
    private String name;
    private String description;
    private Map<String, String> nameTranslations;
    private Map<String, String> descriptionTranslations;
    private Integer level;
    private String path;
    private Boolean isActive;
    private Integer questionCount;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
