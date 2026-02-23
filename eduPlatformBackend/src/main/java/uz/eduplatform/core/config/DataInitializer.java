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

    @Override
    public void run(ApplicationArguments args) {
        String encodedPassword = passwordEncoder.encode("Password1");

        // Find-or-create users
        findOrCreateUser("superadmin@eduplatform.uz", "Super", "Admin", Role.SUPER_ADMIN, encodedPassword);
        User admin = findOrCreateUser("admin@eduplatform.uz", "Admin", "User", Role.ADMIN, encodedPassword);
        findOrCreateUser("moderator@eduplatform.uz", "Moderator", "User", Role.MODERATOR, encodedPassword);
        User teacher = findOrCreateUser("teacher@eduplatform.uz", "Teacher", "User", Role.TEACHER, encodedPassword);
        findOrCreateUser("parent@eduplatform.uz", "Parent", "User", Role.PARENT, encodedPassword);
        findOrCreateUser("student@eduplatform.uz", "Student", "User", Role.STUDENT, encodedPassword);

        // Find-or-create template subjects — returns Matematika
        Subject mathSubject = createDefaultSubjects(admin);

        // Add topics and questions to existing Matematika (no new subject)
        addTeacherTopicsAndQuestions(teacher, mathSubject);

        log.info("Data initialization complete (idempotent — no duplicates)");
    }

    // ─── Users ──────────────────────────────────────────────────────────────────

    private User findOrCreateUser(String email, String firstName, String lastName, Role role, String encodedPassword) {
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

    // ─── Subjects (templates) ───────────────────────────────────────────────────

    private Subject createDefaultSubjects(User adminUser) {
        Subject math = findOrCreateSubject(adminUser, "Matematika", "\u041C\u0430\u0442\u0435\u043C\u0430\u0442\u0438\u043A\u0430", "Mathematics", "\u041C\u0430\u0442\u0435\u043C\u0430\u0442\u0438\u043A\u0430", "\uD83D\uDCD0", 1, true);
        findOrCreateSubject(adminUser, "Fizika", "\u0424\u0438\u0437\u0438\u043A\u0430", "Physics", "\u0424\u0438\u0437\u0438\u043A\u0430", "\u269B\uFE0F", 2, true);
        findOrCreateSubject(adminUser, "Kimyo", "\u041A\u0438\u043C\u0451", "Chemistry", "\u0425\u0438\u043C\u0438\u044F", "\uD83E\uDDEA", 3, true);
        findOrCreateSubject(adminUser, "Biologiya", "\u0411\u0438\u043E\u043B\u043E\u0433\u0438\u044F", "Biology", "\u0411\u0438\u043E\u043B\u043E\u0433\u0438\u044F", "\uD83E\uDDEC", 4, true);
        findOrCreateSubject(adminUser, "Tarix", "\u0422\u0430\u0440\u0438\u0445", "History", "\u0418\u0441\u0442\u043E\u0440\u0438\u044F", "\uD83D\uDCDC", 5, true);
        findOrCreateSubject(adminUser, "Ona tili", "\u041E\u043D\u0430 \u0442\u0438\u043B\u0438", "Uzbek Language", "\u0423\u0437\u0431\u0435\u043A\u0441\u043A\u0438\u0439 \u044F\u0437\u044B\u043A", "\uD83D\uDCDD", 6, true);
        findOrCreateSubject(adminUser, "Ingliz tili", "\u0418\u043D\u0433\u043B\u0438\u0437 \u0442\u0438\u043B\u0438", "English", "\u0410\u043D\u0433\u043B\u0438\u0439\u0441\u043A\u0438\u0439 \u044F\u0437\u044B\u043A", "\uD83C\uDDEC\uD83C\uDDE7", 7, true);
        findOrCreateSubject(adminUser, "Informatika", "\u0418\u043D\u0444\u043E\u0440\u043C\u0430\u0442\u0438\u043A\u0430", "Computer Science", "\u0418\u043D\u0444\u043E\u0440\u043C\u0430\u0442\u0438\u043A\u0430", "\uD83D\uDCBB", 8, true);
        findOrCreateSubject(adminUser, "Geografiya", "\u0413\u0435\u043E\u0433\u0440\u0430\u0444\u0438\u044F", "Geography", "\u0413\u0435\u043E\u0433\u0440\u0430\u0444\u0438\u044F", "\uD83C\uDF0D", 9, true);
        findOrCreateSubject(adminUser, "Adabiyot", "\u0410\u0434\u0430\u0431\u0438\u0451\u0442", "Literature", "\u041B\u0438\u0442\u0435\u0440\u0430\u0442\u0443\u0440\u0430", "\uD83D\uDCDA", 10, true);
        findOrCreateSubject(adminUser, "Rus tili", "\u0420\u0443\u0441 \u0442\u0438\u043B\u0438", "Russian Language", "\u0420\u0443\u0441\u0441\u043A\u0438\u0439 \u044F\u0437\u044B\u043A", "\uD83C\uDDF7\uD83C\uDDFA", 11, true);
        findOrCreateSubject(adminUser, "Huquq asoslari", "\u04B2\u0443\u049B\u0443\u049B \u0430\u0441\u043E\u0441\u043B\u0430\u0440\u0438", "Fundamentals of Law", "\u041E\u0441\u043D\u043E\u0432\u044B \u043F\u0440\u0430\u0432\u0430", "\u2696\uFE0F", 12, true);
        findOrCreateSubject(adminUser, "Iqtisodiyot", "\u0418\u049B\u0442\u0438\u0441\u043E\u0434\u0438\u0451\u0442", "Economics", "\u042D\u043A\u043E\u043D\u043E\u043C\u0438\u043A\u0430", "\uD83D\uDCB0", 13, true);
        findOrCreateSubject(adminUser, "Falsafa", "\u0424\u0430\u043B\u0441\u0430\u0444\u0430", "Philosophy", "\u0424\u0438\u043B\u043E\u0441\u043E\u0444\u0438\u044F", "\uD83E\uDD14", 14, true);
        findOrCreateSubject(adminUser, "Psixologiya", "\u041F\u0441\u0438\u0445\u043E\u043B\u043E\u0433\u0438\u044F", "Psychology", "\u041F\u0441\u0438\u0445\u043E\u043B\u043E\u0433\u0438\u044F", "\uD83E\uDDE0", 15, true);

        log.info("Default template subjects ensured for admin");
        return math;
    }

    private Subject findOrCreateSubject(User owner, String uzLatn, String uzCyrl, String en, String ru,
                                         String icon, int sortOrder, boolean isTemplate) {
        return subjectRepository.findByUserIdAndDefaultName(owner.getId(), uzLatn)
                .orElseGet(() -> {
                    Subject subject = Subject.builder()
                            .user(owner)
                            .name(Map.of("uz_latn", uzLatn, "uz_cyrl", uzCyrl, "en", en, "ru", ru))
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

    // ─── Teacher sample data ────────────────────────────────────────────────────

    private void addTeacherTopicsAndQuestions(User teacher, Subject mathSubject) {
        // Add topics and questions to the existing Matematika template subject (no new subject created)
        Topic algebra = findOrCreateTopic(teacher, mathSubject, "Algebra", "\u0410\u043B\u0433\u0435\u0431\u0440\u0430", "Algebra", "\u0410\u043B\u0433\u0435\u0431\u0440\u0430", 5, 1);
        Topic geometry = findOrCreateTopic(teacher, mathSubject, "Geometriya", "\u0413\u0435\u043E\u043C\u0435\u0442\u0440\u0438\u044F", "Geometry", "\u0413\u0435\u043E\u043C\u0435\u0442\u0440\u0438\u044F", 5, 2);
        Topic arithmetic = findOrCreateTopic(teacher, mathSubject, "Arifmetika", "\u0410\u0440\u0438\u0444\u043C\u0435\u0442\u0438\u043A\u0430", "Arithmetic", "\u0410\u0440\u0438\u0444\u043C\u0435\u0442\u0438\u043A\u0430", 5, 3);

        createAlgebraQuestions(teacher, algebra);
        updateTopicQuestionCount(algebra);

        createGeometryQuestions(teacher, geometry);
        updateTopicQuestionCount(geometry);

        createArithmeticQuestions(teacher, arithmetic);
        updateTopicQuestionCount(arithmetic);

        updateSubjectCounters(mathSubject);

        log.info("Teacher topics & questions ensured for Matematika (3 topics, up to 30 questions)");
    }

    private Topic findOrCreateTopic(User user, Subject subject, String uzLatn, String uzCyrl, String en, String ru,
                                     int gradeLevel, int sortOrder) {
        return topicRepository.findBySubjectIdAndDefaultName(subject.getId(), uzLatn)
                .orElseGet(() -> {
                    Topic topic = Topic.builder()
                            .subject(subject)
                            .user(user)
                            .gradeLevel(gradeLevel)
                            .name(Map.of("uz_latn", uzLatn, "uz_cyrl", uzCyrl, "en", en, "ru", ru))
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

    // ─── Questions ──────────────────────────────────────────────────────────────

    private void createAlgebraQuestions(User teacher, Topic topic) {
        findOrCreateQuestion(teacher, topic, "5 + 3 = ?", Difficulty.EASY,
                opt("8", true), opt("7", false), opt("9", false), opt("6", false));
        findOrCreateQuestion(teacher, topic, "12 - 4 = ?", Difficulty.EASY,
                opt("8", true), opt("7", false), opt("9", false), opt("6", false));
        findOrCreateQuestion(teacher, topic, "x + 5 = 10 tenglamada x = ?", Difficulty.EASY,
                opt("5", true), opt("10", false), opt("15", false), opt("0", false));
        findOrCreateQuestion(teacher, topic, "2 * 6 = ?", Difficulty.EASY,
                opt("12", true), opt("8", false), opt("10", false), opt("14", false));
        findOrCreateQuestion(teacher, topic, "2x + 3 = 11 tenglamada x = ?", Difficulty.MEDIUM,
                opt("4", true), opt("3", false), opt("5", false), opt("7", false));
        findOrCreateQuestion(teacher, topic, "3(x - 2) = 12 tenglamada x = ?", Difficulty.MEDIUM,
                opt("6", true), opt("4", false), opt("8", false), opt("2", false));
        findOrCreateQuestion(teacher, topic, "(a + b)^2 formulasi qaysi?", Difficulty.MEDIUM,
                opt("a\u00B2 + 2ab + b\u00B2", true), opt("a\u00B2 + b\u00B2", false), opt("a\u00B2 - 2ab + b\u00B2", false), opt("2a\u00B2 + 2b\u00B2", false));
        findOrCreateQuestion(teacher, topic, "x\u00B2 - 5x + 6 = 0 tenglamaning ildizlari yig'indisi?", Difficulty.HARD,
                opt("5", true), opt("6", false), opt("-5", false), opt("1", false));
        findOrCreateQuestion(teacher, topic, "x\u00B2 + 4x + 4 = 0 tenglamaning ildizi?", Difficulty.HARD,
                opt("-2", true), opt("2", false), opt("-4", false), opt("4", false));
        findOrCreateQuestion(teacher, topic, "|2x - 6| = 4 tenglamaning yechimlari yig'indisi?", Difficulty.HARD,
                opt("6", true), opt("5", false), opt("4", false), opt("8", false));
    }

    private void createGeometryQuestions(User teacher, Topic topic) {
        findOrCreateQuestion(teacher, topic, "To'rtburchakning burchaklari yig'indisi necha gradus?", Difficulty.EASY,
                opt("360\u00B0", true), opt("180\u00B0", false), opt("270\u00B0", false), opt("90\u00B0", false));
        findOrCreateQuestion(teacher, topic, "Uchburchakning burchaklari yig'indisi?", Difficulty.EASY,
                opt("180\u00B0", true), opt("360\u00B0", false), opt("90\u00B0", false), opt("270\u00B0", false));
        findOrCreateQuestion(teacher, topic, "Kvadratning barcha tomonlari ...", Difficulty.EASY,
                opt("Teng", true), opt("Teng emas", false), opt("Parallel", false), opt("Perpendicular", false));
        findOrCreateQuestion(teacher, topic, "To'g'ri burchak necha gradus?", Difficulty.EASY,
                opt("90\u00B0", true), opt("180\u00B0", false), opt("45\u00B0", false), opt("60\u00B0", false));
        findOrCreateQuestion(teacher, topic, "Tomoni 5 cm bo'lgan kvadratning yuzi?", Difficulty.MEDIUM,
                opt("25 cm\u00B2", true), opt("20 cm\u00B2", false), opt("10 cm\u00B2", false), opt("15 cm\u00B2", false));
        findOrCreateQuestion(teacher, topic, "Radiusi 7 cm bo'lgan doiraning diametri?", Difficulty.MEDIUM,
                opt("14 cm", true), opt("7 cm", false), opt("21 cm", false), opt("3.5 cm", false));
        findOrCreateQuestion(teacher, topic, "Asosi 8, balandligi 5 bo'lgan uchburchak yuzi?", Difficulty.MEDIUM,
                opt("20", true), opt("40", false), opt("13", false), opt("30", false));
        findOrCreateQuestion(teacher, topic, "Gipotenuzasi 13, bir kateti 5 bo'lgan to'g'ri burchakli uchburchakning ikkinchi kateti?", Difficulty.HARD,
                opt("12", true), opt("8", false), opt("10", false), opt("11", false));
        findOrCreateQuestion(teacher, topic, "Radiusi 10 cm bo'lgan doira yuzi? (\u03C0 \u2248 3.14)", Difficulty.HARD,
                opt("314 cm\u00B2", true), opt("31.4 cm\u00B2", false), opt("62.8 cm\u00B2", false), opt("100 cm\u00B2", false));
        findOrCreateQuestion(teacher, topic, "Teng yonli uchburchakning asosi 10, yon tomoni 13. Balandligi?", Difficulty.HARD,
                opt("12", true), opt("8", false), opt("10", false), opt("15", false));
    }

    private void createArithmeticQuestions(User teacher, Topic topic) {
        findOrCreateQuestion(teacher, topic, "25 + 37 = ?", Difficulty.EASY,
                opt("62", true), opt("52", false), opt("72", false), opt("63", false));
        findOrCreateQuestion(teacher, topic, "100 - 45 = ?", Difficulty.EASY,
                opt("55", true), opt("65", false), opt("45", false), opt("50", false));
        findOrCreateQuestion(teacher, topic, "8 * 7 = ?", Difficulty.EASY,
                opt("56", true), opt("48", false), opt("63", false), opt("54", false));
        findOrCreateQuestion(teacher, topic, "72 / 8 = ?", Difficulty.EASY,
                opt("9", true), opt("8", false), opt("7", false), opt("6", false));
        findOrCreateQuestion(teacher, topic, "12, 18 va 24 sonlarining EKUB?", Difficulty.MEDIUM,
                opt("6", true), opt("3", false), opt("12", false), opt("2", false));
        findOrCreateQuestion(teacher, topic, "3/4 + 1/2 = ?", Difficulty.MEDIUM,
                opt("5/4", true), opt("4/6", false), opt("1/2", false), opt("3/2", false));
        findOrCreateQuestion(teacher, topic, "0.25 * 0.4 = ?", Difficulty.MEDIUM,
                opt("0.1", true), opt("0.01", false), opt("1.0", false), opt("0.65", false));
        findOrCreateQuestion(teacher, topic, "2\u2075 + 3\u00B3 = ?", Difficulty.HARD,
                opt("59", true), opt("41", false), opt("35", false), opt("67", false));
        findOrCreateQuestion(teacher, topic, "\u221A144 + \u221A81 = ?", Difficulty.HARD,
                opt("21", true), opt("15", false), opt("18", false), opt("25", false));
        findOrCreateQuestion(teacher, topic, "123 * 456 ning oxirgi raqami?", Difficulty.HARD,
                opt("8", true), opt("6", false), opt("2", false), opt("4", false));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private void findOrCreateQuestion(User teacher, Topic topic, String questionText, Difficulty difficulty,
                                       String optA, String optB, String optC, String optD) {
        // Check if question already exists by topic + text
        if (questionRepository.existsByTopicIdAndDefaultQuestionText(topic.getId(), questionText)) {
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
                    .questionText(Map.of("uz_latn", questionText, "en", questionText))
                    .questionType(QuestionType.MCQ_SINGLE)
                    .difficulty(difficulty)
                    .points(BigDecimal.ONE)
                    .timeLimitSeconds(60)
                    .options(options)
                    .correctAnswer("\"" + correctAnswer + "\"")
                    .proof(Map.of("uz_latn", "Hisoblab tekshirilgan"))
                    .proofRequired(true)
                    .status(QuestionStatus.DRAFT)
                    .build();

            questionRepository.save(question);
        } catch (Exception e) {
            log.warn("Failed to create sample question: {}", e.getMessage());
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

    private String opt(String text, boolean isCorrect) {
        try {
            Map<String, Object> option = Map.of(
                    "id", java.util.UUID.randomUUID().toString(),
                    "text", Map.of("uz_latn", text, "en", text),
                    "isCorrect", isCorrect
            );
            return objectMapper.writeValueAsString(option);
        } catch (Exception e) {
            return "";
        }
    }
}
