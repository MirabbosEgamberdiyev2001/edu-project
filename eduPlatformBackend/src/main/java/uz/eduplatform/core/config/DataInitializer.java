package uz.eduplatform.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.domain.UserStatus;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.content.domain.*;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.content.repository.SubjectRepository;
import uz.eduplatform.modules.content.repository.TopicRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Idempotent data initializer for dev environment.
 * Safe to run multiple times — uses find-or-create for every entity.
 * Never creates duplicates.
 *
 * All textual content is provided in 4 languages: uz_latn, uz_cyrl, en, ru.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    // ─── Proof translations ───────────────────────────────────────────────────

    private static final Map<String, String> PROOF_ALGEBRA = Map.of(
            "uz_latn", "Tenglama ikkala tomoniga bir xil amal qo'llanib yechildi va natija tekshirildi",
            "uz_cyrl", "Тенглама иккала томонига бир хил амал қўлланиб ечилди ва натижа текширилди",
            "en",      "The equation was solved by applying equal operations to both sides and the result was verified",
            "ru",      "Уравнение решено путём применения одинаковых операций к обеим сторонам, результат проверен"
    );

    private static final Map<String, String> PROOF_GEOMETRY = Map.of(
            "uz_latn", "Geometrik formulalar va teoremalar yordamida hisoblab topildi va tekshirildi",
            "uz_cyrl", "Геометрик формулалар ва теоремалар ёрдамида ҳисоблаб топилди ва текширилди",
            "en",      "Calculated and verified using geometric formulas and theorems",
            "ru",      "Вычислено и проверено с использованием геометрических формул и теорем"
    );

    private static final Map<String, String> PROOF_ARITHMETIC = Map.of(
            "uz_latn", "Arifmetik amallar orqali hisob-kitob qilib tekshirildi",
            "uz_cyrl", "Арифметик амаллар орқали ҳисоб-китоб қилиб текширилди",
            "en",      "Verified through step-by-step arithmetic operations",
            "ru",      "Проверено пошаговыми арифметическими действиями"
    );

    // ─── Entry point ──────────────────────────────────────────────────────────

    @Override
    public void run(ApplicationArguments args) {
        String encodedPassword = passwordEncoder.encode("Password1");

        findOrCreateUser("superadmin@eduplatform.uz", "Super", "Admin", Role.SUPER_ADMIN, encodedPassword);
        User admin = findOrCreateUser("admin@eduplatform.uz", "Admin", "User", Role.ADMIN, encodedPassword);
        findOrCreateUser("moderator@eduplatform.uz", "Moderator", "User", Role.MODERATOR, encodedPassword);
        User teacher = findOrCreateUser("teacher@eduplatform.uz", "Teacher", "User", Role.TEACHER, encodedPassword);
        findOrCreateUser("parent@eduplatform.uz", "Parent", "User", Role.PARENT, encodedPassword);
        findOrCreateUser("student@eduplatform.uz", "Student", "User", Role.STUDENT, encodedPassword);

        Subject mathSubject = createDefaultSubjects(admin);
        addTeacherTopicsAndQuestions(teacher, mathSubject);

        log.info("Data initialization complete (idempotent — no duplicates)");
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    private User findOrCreateUser(String email, String firstName, String lastName,
                                   Role role, String encodedPassword) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .role(role)
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .passwordHash(encodedPassword)
                    .passwordChangedAt(LocalDateTime.now())
                    .build();
            user = userRepository.save(user);
            log.info("Created user: {} ({})", email, role);
            return user;
        });
    }

    // ─── Subjects ─────────────────────────────────────────────────────────────

    private Subject createDefaultSubjects(User adminUser) {
        Subject math = findOrCreateSubject(adminUser,
                "Matematika",   "Математика",   "Mathematics",      "Математика",
                "📐", 1, true);
        findOrCreateSubject(adminUser,
                "Fizika",       "Физика",       "Physics",          "Физика",
                "⚛️", 2, true);
        findOrCreateSubject(adminUser,
                "Kimyo",        "Кимё",         "Chemistry",        "Химия",
                "🧪", 3, true);
        findOrCreateSubject(adminUser,
                "Biologiya",    "Биология",     "Biology",          "Биология",
                "🧬", 4, true);
        findOrCreateSubject(adminUser,
                "Tarix",        "Тарих",        "History",          "История",
                "📜", 5, true);
        findOrCreateSubject(adminUser,
                "Ona tili",     "Она тили",     "Uzbek Language",   "Узбекский язык",
                "📝", 6, true);
        findOrCreateSubject(adminUser,
                "Ingliz tili",  "Инглиз тили",  "English",          "Английский язык",
                "🇬🇧", 7, true);
        findOrCreateSubject(adminUser,
                "Informatika",  "Информатика",  "Computer Science", "Информатика",
                "💻", 8, true);
        findOrCreateSubject(adminUser,
                "Geografiya",   "География",    "Geography",        "География",
                "🌍", 9, true);
        findOrCreateSubject(adminUser,
                "Adabiyot",     "Адабиёт",      "Literature",       "Литература",
                "📚", 10, true);
        findOrCreateSubject(adminUser,
                "Rus tili",     "Рус тили",     "Russian Language", "Русский язык",
                "🇷🇺", 11, true);
        findOrCreateSubject(adminUser,
                "Huquq asoslari", "Ҳуқуқ асослари", "Fundamentals of Law", "Основы права",
                "⚖️", 12, true);
        findOrCreateSubject(adminUser,
                "Iqtisodiyot",  "Иқтисодиёт",  "Economics",        "Экономика",
                "💰", 13, true);
        findOrCreateSubject(adminUser,
                "Falsafa",      "Фалсафа",      "Philosophy",       "Философия",
                "🤔", 14, true);
        findOrCreateSubject(adminUser,
                "Psixologiya",  "Психология",   "Psychology",       "Психология",
                "🧠", 15, true);

        log.info("Default template subjects ensured");
        return math;
    }

    private Subject findOrCreateSubject(User owner,
                                         String uzLatn, String uzCyrl, String en, String ru,
                                         String icon, int sortOrder, boolean isTemplate) {
        return subjectRepository.findByUserIdAndDefaultName(owner.getId(), uzLatn)
                .orElseGet(() -> {
                    Subject subject = Subject.builder()
                            .user(owner)
                            .name(ml(uzLatn, uzCyrl, en, ru))
                            .description(Map.of())
                            .icon(icon)
                            .isTemplate(isTemplate)
                            .isActive(true)
                            .isArchived(false)
                            .sortOrder(sortOrder)
                            .build();
                    subject = subjectRepository.save(subject);
                    log.info("Created subject: {} (template={})", uzLatn, isTemplate);
                    return subject;
                });
    }

    // ─── Topics & Questions ───────────────────────────────────────────────────

    private void addTeacherTopicsAndQuestions(User teacher, Subject mathSubject) {
        Topic algebra = findOrCreateTopic(teacher, mathSubject,
                "Algebra",      "Алгебра",      "Algebra",      "Алгебра",      5, 1);
        Topic geometry = findOrCreateTopic(teacher, mathSubject,
                "Geometriya",   "Геометрия",    "Geometry",     "Геометрия",    5, 2);
        Topic arithmetic = findOrCreateTopic(teacher, mathSubject,
                "Arifmetika",   "Арифметика",   "Arithmetic",   "Арифметика",   5, 3);

        createAlgebraQuestions(teacher, algebra);
        updateTopicQuestionCount(algebra);

        createGeometryQuestions(teacher, geometry);
        updateTopicQuestionCount(geometry);

        createArithmeticQuestions(teacher, arithmetic);
        updateTopicQuestionCount(arithmetic);

        updateSubjectCounters(mathSubject);
        log.info("Teacher topics & questions ensured (3 topics, 30 questions, 4 languages)");
    }

    private Topic findOrCreateTopic(User user, Subject subject,
                                     String uzLatn, String uzCyrl, String en, String ru,
                                     int gradeLevel, int sortOrder) {
        return topicRepository.findBySubjectIdAndDefaultName(subject.getId(), uzLatn)
                .orElseGet(() -> {
                    Topic topic = Topic.builder()
                            .subject(subject)
                            .user(user)
                            .gradeLevel(gradeLevel)
                            .name(ml(uzLatn, uzCyrl, en, ru))
                            .description(Map.of())
                            .level(1)
                            .isActive(true)
                            .sortOrder(sortOrder)
                            .build();
                    topic = topicRepository.save(topic);
                    log.info("Created topic: {} (grade {})", uzLatn, gradeLevel);
                    return topic;
                });
    }

    // ─── Algebra questions (10) ───────────────────────────────────────────────

    private void createAlgebraQuestions(User teacher, Topic topic) {
        // Pure math — same in all languages
        q(teacher, topic, mlSame("5 + 3 = ?"), Difficulty.EASY, PROOF_ALGEBRA,
                opt("8", true), opt("7", false), opt("9", false), opt("6", false));

        q(teacher, topic, mlSame("12 - 4 = ?"), Difficulty.EASY, PROOF_ALGEBRA,
                opt("8", true), opt("7", false), opt("9", false), opt("6", false));

        q(teacher, topic,
                ml("x + 5 = 10 tenglamada x = ?",
                   "x + 5 = 10 тенгламада x = ?",
                   "Find x in equation: x + 5 = 10",
                   "Найдите x из уравнения: x + 5 = 10"),
                Difficulty.EASY, PROOF_ALGEBRA,
                opt("5", true), opt("10", false), opt("15", false), opt("0", false));

        q(teacher, topic, mlSame("2 × 6 = ?"), Difficulty.EASY, PROOF_ALGEBRA,
                opt("12", true), opt("8", false), opt("10", false), opt("14", false));

        q(teacher, topic,
                ml("2x + 3 = 11 tenglamada x = ?",
                   "2x + 3 = 11 тенгламада x = ?",
                   "Find x in equation: 2x + 3 = 11",
                   "Найдите x из уравнения: 2x + 3 = 11"),
                Difficulty.MEDIUM, PROOF_ALGEBRA,
                opt("4", true), opt("3", false), opt("5", false), opt("7", false));

        q(teacher, topic,
                ml("3(x - 2) = 12 tenglamada x = ?",
                   "3(x - 2) = 12 тенгламада x = ?",
                   "Find x in equation: 3(x - 2) = 12",
                   "Найдите x из уравнения: 3(x - 2) = 12"),
                Difficulty.MEDIUM, PROOF_ALGEBRA,
                opt("6", true), opt("4", false), opt("8", false), opt("2", false));

        q(teacher, topic,
                ml("(a + b)² formulasi qaysi?",
                   "(a + b)² формуласи қайси?",
                   "Which is the formula for (a + b)²?",
                   "Какова формула для (a + b)²?"),
                Difficulty.MEDIUM, PROOF_ALGEBRA,
                opt("a² + 2ab + b²", true),
                opt("a² + b²", false),
                opt("a² − 2ab + b²", false),
                opt("2a² + 2b²", false));

        q(teacher, topic,
                ml("x² − 5x + 6 = 0 tenglamaning ildizlari yig'indisi?",
                   "x² − 5x + 6 = 0 тенгламанинг илдизлари йиғиндиси?",
                   "Sum of roots of equation x² − 5x + 6 = 0?",
                   "Сумма корней уравнения x² − 5x + 6 = 0?"),
                Difficulty.HARD, PROOF_ALGEBRA,
                opt("5", true), opt("6", false), opt("−5", false), opt("1", false));

        q(teacher, topic,
                ml("x² + 4x + 4 = 0 tenglamaning ildizi?",
                   "x² + 4x + 4 = 0 тенгламанинг илдизи?",
                   "Root of equation x² + 4x + 4 = 0?",
                   "Корень уравнения x² + 4x + 4 = 0?"),
                Difficulty.HARD, PROOF_ALGEBRA,
                opt("−2", true), opt("2", false), opt("−4", false), opt("4", false));

        q(teacher, topic,
                ml("|2x − 6| = 4 tenglamaning yechimlari yig'indisi?",
                   "|2x − 6| = 4 тенгламанинг ечимлари йиғиндиси?",
                   "Sum of solutions of |2x − 6| = 4?",
                   "Сумма решений уравнения |2x − 6| = 4?"),
                Difficulty.HARD, PROOF_ALGEBRA,
                opt("6", true), opt("5", false), opt("4", false), opt("8", false));
    }

    // ─── Geometry questions (10) ──────────────────────────────────────────────

    private void createGeometryQuestions(User teacher, Topic topic) {
        q(teacher, topic,
                ml("To'rtburchakning burchaklari yig'indisi necha gradus?",
                   "Тўртбурчакнинг бурчаклари йиғиндиси неча градус?",
                   "How many degrees is the sum of angles in a quadrilateral?",
                   "Чему равна сумма углов четырёхугольника в градусах?"),
                Difficulty.EASY, PROOF_GEOMETRY,
                opt("360°", true), opt("180°", false), opt("270°", false), opt("90°", false));

        q(teacher, topic,
                ml("Uchburchakning burchaklari yig'indisi?",
                   "Учбурчакнинг бурчаклари йиғиндиси?",
                   "What is the sum of angles of a triangle?",
                   "Чему равна сумма углов треугольника?"),
                Difficulty.EASY, PROOF_GEOMETRY,
                opt("180°", true), opt("360°", false), opt("90°", false), opt("270°", false));

        q(teacher, topic,
                ml("Kvadratning barcha tomonlari ...",
                   "Квадратнинг барча томонлари ...",
                   "All sides of a square are ...",
                   "Все стороны квадрата ..."),
                Difficulty.EASY, PROOF_GEOMETRY,
                opt("Teng",       "Тенг",       "Equal",        "Равны",          true),
                opt("Teng emas",  "Тенг эмас",  "Not equal",    "Не равны",       false),
                opt("Parallel",   "Параллел",   "Parallel",     "Параллельны",    false),
                opt("Perpendikulyar", "Перпендикуляр", "Perpendicular", "Перпендикулярны", false));

        q(teacher, topic,
                ml("To'g'ri burchak necha gradus?",
                   "Тўғри бурчак неча градус?",
                   "How many degrees is a right angle?",
                   "Сколько градусов в прямом угле?"),
                Difficulty.EASY, PROOF_GEOMETRY,
                opt("90°", true), opt("180°", false), opt("45°", false), opt("60°", false));

        q(teacher, topic,
                ml("Tomoni 5 cm bo'lgan kvadratning yuzi?",
                   "Томони 5 см бўлган квадратнинг юзи?",
                   "Area of a square with side 5 cm?",
                   "Площадь квадрата со стороной 5 см?"),
                Difficulty.MEDIUM, PROOF_GEOMETRY,
                opt("25 cm²", true), opt("20 cm²", false), opt("10 cm²", false), opt("15 cm²", false));

        q(teacher, topic,
                ml("Radiusi 7 cm bo'lgan doiraning diametri?",
                   "Радиуси 7 см бўлган доиранинг диаметри?",
                   "Diameter of a circle with radius 7 cm?",
                   "Диаметр окружности с радиусом 7 см?"),
                Difficulty.MEDIUM, PROOF_GEOMETRY,
                opt("14 cm", true), opt("7 cm", false), opt("21 cm", false), opt("3,5 cm", false));

        q(teacher, topic,
                ml("Asosi 8, balandligi 5 bo'lgan uchburchak yuzi?",
                   "Асоси 8, баландлиги 5 бўлган учбурчак юзи?",
                   "Area of a triangle with base 8 and height 5?",
                   "Площадь треугольника с основанием 8 и высотой 5?"),
                Difficulty.MEDIUM, PROOF_GEOMETRY,
                opt("20", true), opt("40", false), opt("13", false), opt("30", false));

        q(teacher, topic,
                ml("Gipotenuzasi 13, bir kateti 5 bo'lgan to'g'ri burchakli uchburchakning ikkinchi kateti?",
                   "Гипотенузаси 13, бир катети 5 бўлган тўғри бурчакли учбурчакнинг иккинчи катети?",
                   "Second leg of a right triangle with hypotenuse 13 and one leg 5?",
                   "Второй катет прямоугольного треугольника с гипотенузой 13 и одним катетом 5?"),
                Difficulty.HARD, PROOF_GEOMETRY,
                opt("12", true), opt("8", false), opt("10", false), opt("11", false));

        q(teacher, topic,
                ml("Radiusi 10 cm bo'lgan doira yuzi? (π ≈ 3,14)",
                   "Радиуси 10 см бўлган доира юзи? (π ≈ 3,14)",
                   "Area of a circle with radius 10 cm? (π ≈ 3.14)",
                   "Площадь круга с радиусом 10 см? (π ≈ 3,14)"),
                Difficulty.HARD, PROOF_GEOMETRY,
                opt("314 cm²", true), opt("31,4 cm²", false), opt("62,8 cm²", false), opt("100 cm²", false));

        q(teacher, topic,
                ml("Teng yonli uchburchakning asosi 10, yon tomoni 13. Balandligi?",
                   "Тенг ёнли учбурчакнинг асоси 10, ён томони 13. Баландлиги?",
                   "Isosceles triangle: base 10, lateral side 13. What is its height?",
                   "Равнобедренный треугольник: основание 10, боковая сторона 13. Высота?"),
                Difficulty.HARD, PROOF_GEOMETRY,
                opt("12", true), opt("8", false), opt("10", false), opt("15", false));
    }

    // ─── Arithmetic questions (10) ────────────────────────────────────────────

    private void createArithmeticQuestions(User teacher, Topic topic) {
        q(teacher, topic, mlSame("25 + 37 = ?"), Difficulty.EASY, PROOF_ARITHMETIC,
                opt("62", true), opt("52", false), opt("72", false), opt("63", false));

        q(teacher, topic, mlSame("100 − 45 = ?"), Difficulty.EASY, PROOF_ARITHMETIC,
                opt("55", true), opt("65", false), opt("45", false), opt("50", false));

        q(teacher, topic, mlSame("8 × 7 = ?"), Difficulty.EASY, PROOF_ARITHMETIC,
                opt("56", true), opt("48", false), opt("63", false), opt("54", false));

        q(teacher, topic, mlSame("72 ÷ 8 = ?"), Difficulty.EASY, PROOF_ARITHMETIC,
                opt("9", true), opt("8", false), opt("7", false), opt("6", false));

        q(teacher, topic,
                ml("12, 18 va 24 sonlarining EKUB?",
                   "12, 18 ва 24 сонларининг ЭКУБ?",
                   "GCD of 12, 18 and 24?",
                   "НОД чисел 12, 18 и 24?"),
                Difficulty.MEDIUM, PROOF_ARITHMETIC,
                opt("6", true), opt("3", false), opt("12", false), opt("2", false));

        q(teacher, topic, mlSame("3/4 + 1/2 = ?"), Difficulty.MEDIUM, PROOF_ARITHMETIC,
                opt("5/4", true), opt("4/6", false), opt("1/2", false), opt("3/2", false));

        q(teacher, topic, mlSame("0,25 × 0,4 = ?"), Difficulty.MEDIUM, PROOF_ARITHMETIC,
                opt("0,1", true), opt("0,01", false), opt("1,0", false), opt("0,65", false));

        q(teacher, topic, mlSame("2⁵ + 3³ = ?"), Difficulty.HARD, PROOF_ARITHMETIC,
                opt("59", true), opt("41", false), opt("35", false), opt("67", false));

        q(teacher, topic, mlSame("√144 + √81 = ?"), Difficulty.HARD, PROOF_ARITHMETIC,
                opt("21", true), opt("15", false), opt("18", false), opt("25", false));

        q(teacher, topic,
                ml("123 × 456 ning oxirgi raqami?",
                   "123 × 456 нинг охирги рақами?",
                   "What is the last digit of 123 × 456?",
                   "Какова последняя цифра произведения 123 × 456?"),
                Difficulty.HARD, PROOF_ARITHMETIC,
                opt("8", true), opt("6", false), opt("2", false), opt("4", false));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Creates a multilingual text map with all 4 supported locales.
     */
    private static Map<String, String> ml(String uzL, String uzC, String en, String ru) {
        return Map.of("uz_latn", uzL, "uz_cyrl", uzC, "en", en, "ru", ru);
    }

    /**
     * Same text in all 4 languages (for math notation, numbers, formulas).
     */
    private static Map<String, String> mlSame(String text) {
        return ml(text, text, text, text);
    }

    /**
     * Creates an MCQ option with all 4 language translations.
     */
    private String opt(String uzL, String uzC, String en, String ru, boolean isCorrect) {
        try {
            Map<String, Object> option = Map.of(
                    "id",        java.util.UUID.randomUUID().toString(),
                    "text",      ml(uzL, uzC, en, ru),
                    "isCorrect", isCorrect
            );
            return objectMapper.writeValueAsString(option);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Creates an MCQ option with same text in all 4 languages
     * (for numeric answers, math notation, universal symbols).
     */
    private String opt(String text, boolean isCorrect) {
        return opt(text, text, text, text, isCorrect);
    }

    /**
     * Convenience alias for findOrCreateQuestion (shorter name for readability).
     */
    private void q(User teacher, Topic topic,
                   Map<String, String> questionText,
                   Difficulty difficulty,
                   Map<String, String> proof,
                   String optA, String optB, String optC, String optD) {
        String uzL = questionText.get("uz_latn");
        if (questionRepository.existsByTopicIdAndDefaultQuestionText(topic.getId(), uzL)) {
            return;
        }
        try {
            String options = "[" + optA + "," + optB + "," + optC + "," + optD + "]";

            String correctAnswer = "A";
            List<?> optList = objectMapper.readValue(options, List.class);
            for (int i = 0; i < optList.size(); i++) {
                Map<?, ?> o = (Map<?, ?>) optList.get(i);
                if (Boolean.TRUE.equals(o.get("isCorrect"))) {
                    correctAnswer = String.valueOf((char) ('A' + i));
                    break;
                }
            }

            Question question = Question.builder()
                    .topic(topic)
                    .user(teacher)
                    .questionText(questionText)
                    .questionType(QuestionType.MCQ_SINGLE)
                    .difficulty(difficulty)
                    .points(BigDecimal.ONE)
                    .timeLimitSeconds(60)
                    .options(options)
                    .correctAnswer("\"" + correctAnswer + "\"")
                    .proof(proof)
                    .proofRequired(true)
                    .status(QuestionStatus.DRAFT)
                    .build();

            questionRepository.save(question);
        } catch (Exception e) {
            log.warn("Failed to create sample question '{}': {}", uzL, e.getMessage());
        }
    }

    private void updateTopicQuestionCount(Topic topic) {
        long count = questionRepository.countByTopicId(topic.getId());
        topic.setQuestionCount((int) count);
        topicRepository.save(topic);
    }

    private void updateSubjectCounters(Subject subject) {
        long topicCount = topicRepository.countBySubjectId(subject.getId());
        subject.setTopicCount((int) topicCount);
        Integer questionCount = topicRepository.sumQuestionCountBySubjectId(subject.getId()).orElse(0);
        subject.setQuestionCount(questionCount);
        subjectRepository.save(subject);
    }
}
