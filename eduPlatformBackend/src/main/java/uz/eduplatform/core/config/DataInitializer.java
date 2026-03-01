package uz.eduplatform.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.domain.UserStatus;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.content.domain.Subject;
import uz.eduplatform.modules.content.repository.SubjectRepository;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Idempotent data initializer — runs in ALL profiles (dev, prod, etc.).
 * Safe to run multiple times — uses find-or-create for every entity.
 * Never creates duplicates.
 *
 * Default credentials (change after first login in production!):
 *   superadmin@eduplatform.uz / Super@Admin1
 *   admin@eduplatform.uz      / Admin@1234
 *   moderator@eduplatform.uz  / Moder@1234
 *   teacher@eduplatform.uz    / Teach@1234
 *   parent@eduplatform.uz     / Parent@1234
 *   student@eduplatform.uz    / Stud@1234
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubjectRepository subjectRepository;

    @Override
    public void run(ApplicationArguments args) {
        User admin = initUsers();
        initSubjects(admin);
        log.info("Data initialization complete (idempotent)");
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    private User initUsers() {
        findOrCreateUser("superadmin@eduplatform.uz", "Super",     "Admin", Role.SUPER_ADMIN, "Super@Admin1");
        User admin =
        findOrCreateUser("admin@eduplatform.uz",      "Admin",     "User",  Role.ADMIN,       "Admin@1234");
        findOrCreateUser("moderator@eduplatform.uz",  "Moderator", "User",  Role.MODERATOR,   "Moder@1234");
        findOrCreateUser("teacher@eduplatform.uz",    "Teacher",   "User",  Role.TEACHER,     "Teach@1234");
        findOrCreateUser("parent@eduplatform.uz",     "Parent",    "User",  Role.PARENT,      "Parent@1234");
        findOrCreateUser("student@eduplatform.uz",    "Student",   "User",  Role.STUDENT,     "Stud@1234");
        return admin;
    }

    private User findOrCreateUser(String email, String firstName, String lastName,
                                   Role role, String rawPassword) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .role(role)
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .passwordChangedAt(LocalDateTime.now())
                    .build();
            user = userRepository.save(user);
            log.info("Created default user: {} ({})", email, role);
            return user;
        });
    }

    // ─── Subjects ─────────────────────────────────────────────────────────────

    private void initSubjects(User owner) {
        findOrCreateSubject(owner, "Matematika",     "Математика",      "Mathematics",         "Математика",      "📐", 1);
        findOrCreateSubject(owner, "Fizika",         "Физика",          "Physics",             "Физика",          "⚛️", 2);
        findOrCreateSubject(owner, "Kimyo",          "Кимё",            "Chemistry",           "Химия",           "🧪", 3);
        findOrCreateSubject(owner, "Biologiya",      "Биология",        "Biology",             "Биология",        "🧬", 4);
        findOrCreateSubject(owner, "Tarix",          "Тарих",           "History",             "История",         "📜", 5);
        findOrCreateSubject(owner, "Ona tili",       "Она тили",        "Uzbek Language",      "Узбекский язык",  "📝", 6);
        findOrCreateSubject(owner, "Ingliz tili",    "Инглиз тили",     "English",             "Английский язык", "🇬🇧", 7);
        findOrCreateSubject(owner, "Informatika",    "Информатика",     "Computer Science",    "Информатика",     "💻", 8);
        findOrCreateSubject(owner, "Geografiya",     "География",       "Geography",           "География",       "🌍", 9);
        findOrCreateSubject(owner, "Adabiyot",       "Адабиёт",         "Literature",          "Литература",      "📚", 10);
        findOrCreateSubject(owner, "Rus tili",       "Рус тили",        "Russian Language",    "Русский язык",    "🇷🇺", 11);
        findOrCreateSubject(owner, "Huquq asoslari", "Ҳуқуқ асослари", "Fundamentals of Law", "Основы права",    "⚖️", 12);
        findOrCreateSubject(owner, "Iqtisodiyot",    "Иқтисодиёт",     "Economics",           "Экономика",       "💰", 13);
        findOrCreateSubject(owner, "Falsafa",        "Фалсафа",         "Philosophy",          "Философия",       "🤔", 14);
        findOrCreateSubject(owner, "Psixologiya",    "Психология",      "Psychology",          "Психология",      "🧠", 15);
        log.info("Default subjects ensured (15 subjects, owner: {})", owner.getEmail());
    }

    private void findOrCreateSubject(User owner,
                                      String uzLatn, String uzCyrl, String en, String ru,
                                      String icon, int sortOrder) {
        subjectRepository.findByUserIdAndDefaultName(owner.getId(), uzLatn)
                .orElseGet(() -> {
                    Subject subject = Subject.builder()
                            .user(owner)
                            .name(Map.of("uz_latn", uzLatn, "uz_cyrl", uzCyrl, "en", en, "ru", ru))
                            .description(Map.of())
                            .icon(icon)
                            .isTemplate(true)
                            .isActive(true)
                            .isArchived(false)
                            .sortOrder(sortOrder)
                            .build();
                    subject = subjectRepository.save(subject);
                    log.info("Created subject: {}", uzLatn);
                    return subject;
                });
    }
}
