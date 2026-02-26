package uz.eduplatform.modules.test.domain;

public enum GlobalStatus {
    NONE,               // Default - only visible to the teacher
    PENDING_MODERATION, // Teacher submitted for global, waiting for moderator
    APPROVED,           // Moderator approved - visible to ALL roles
    REJECTED            // Moderator rejected - teacher can resubmit after fixing
}
