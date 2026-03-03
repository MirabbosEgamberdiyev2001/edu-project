package uz.eduplatform.modules.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO for public landing page — no sensitive fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicSubjectDto {

    /** Localized subject name (based on request lang param). */
    private String name;

    /** Localized short description. May be null. */
    private String description;

    /** Number of tests available for this subject. */
    private Integer testCount;

    /** Emoji or icon identifier. May be null. */
    private String icon;

    /** Hex color code (e.g. "#1565c0"). May be null. */
    private String color;
}
