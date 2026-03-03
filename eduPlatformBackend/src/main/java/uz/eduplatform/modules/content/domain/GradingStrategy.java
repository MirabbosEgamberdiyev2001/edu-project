package uz.eduplatform.modules.content.domain;

/**
 * Grading strategy for SHORT_ANSWER and FILL_BLANK questions.
 *
 * EXACT_MATCH  – student answer must match one of the correct answers exactly
 *                (case-insensitive, trimmed).
 * CONTAINS     – student answer must contain the correct answer substring
 *                (case-insensitive).
 * MANUAL       – always routed to teacher for manual grading.
 */
public enum GradingStrategy {
    EXACT_MATCH,
    CONTAINS,
    MANUAL
}
