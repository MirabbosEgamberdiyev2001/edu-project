package uz.eduplatform.modules.content.domain;

public enum QuestionType {
    MCQ_SINGLE,      // Bir javobli (4 variant, 1 to'g'ri)
    MCQ_MULTI,       // Ko'p javobli (partial credit)
    TRUE_FALSE,      // To'g'ri/Noto'g'ri
    FILL_BLANK,      // Bo'sh joy to'ldirish
    MATCHING,        // Moslashtirish
    ORDERING,        // Tartibga keltirish
    SHORT_ANSWER,    // Qisqa javob
    ESSAY            // Insho
}
