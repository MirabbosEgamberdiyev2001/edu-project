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
 *   superadmin@test-pro.uz / Super@Admin1
 *   admin@test-pro.uz      / Admin@1234
 *   moderator@test-pro.uz  / Moder@1234
 *   teacher@test-pro.uz    / Teach@1234
 *   parent@test-pro.uz     / Parent@1234
 *   student@test-pro.uz    / Stud@1234
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
        findOrCreateUser("superadmin@test-pro.uz", "Super",     "Admin", Role.SUPER_ADMIN, "Super@Admin1");
        User admin =
        findOrCreateUser("admin@test-pro.uz",      "Admin",     "User",  Role.ADMIN,       "Admin@1234");
        findOrCreateUser("moderator@test-pro.uz",  "Moderator", "User",  Role.MODERATOR,   "Moder@1234");
        findOrCreateUser("teacher@test-pro.uz",    "Teacher",   "User",  Role.TEACHER,     "Teach@1234");
        findOrCreateUser("parent@test-pro.uz",     "Parent",    "User",  Role.PARENT,      "Parent@1234");
        findOrCreateUser("student@test-pro.uz",    "Student",   "User",  Role.STUDENT,     "Stud@1234");
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
        findOrCreateSubject(owner,
                "Matematika", "Математика", "Mathematics", "Математика",
                ml("Algebra, geometriya, arifmetika, analiz va boshqa matematik sohalarni qamrab oluvchi fan",
                   "Алгебра, геометрия, арифметика, анализ ва бошқа математик соҳаларни қамраб олувчи фан",
                   "A subject covering algebra, geometry, arithmetic, calculus and other mathematical fields",
                   "Предмет, охватывающий алгебру, геометрию, арифметику, анализ и другие математические области"),
                "📐", 1);

        findOrCreateSubject(owner,
                "Fizika", "Физика", "Physics", "Физика",
                ml("Moddaning xossalari, harakat, energiya va koinotning fundamental qonunlarini o'rganuvchi fan",
                   "Модданинг хоссалари, ҳаракат, энергия ва коинотнинг фундаментал қонунларини ўрганувчи фан",
                   "The study of matter, motion, energy and the fundamental laws of the universe",
                   "Изучение свойств материи, движения, энергии и фундаментальных законов вселенной"),
                "⚛️", 2);

        findOrCreateSubject(owner,
                "Kimyo", "Кимё", "Chemistry", "Химия",
                ml("Moddaning tarkibi, tuzilishi, xossalari va o'zgarishlarini o'rganuvchi tabiat fani",
                   "Модданинг таркиби, тузилиши, хоссалари ва ўзгаришларини ўрганувчи табиат фани",
                   "The natural science studying the composition, structure, properties and changes of matter",
                   "Естественная наука, изучающая состав, строение, свойства и превращения веществ"),
                "🧪", 3);

        findOrCreateSubject(owner,
                "Biologiya", "Биология", "Biology", "Биология",
                ml("Tirik organizmlar, ularning tuzilishi, rivojlanishi va muhit bilan o'zaro ta'sirini o'rganuvchi fan",
                   "Тирик организмлар, уларнинг тузилиши, ривожланиши ва муҳит билан ўзаро таъсирини ўрганувчи фан",
                   "The science of living organisms, their structure, development and interaction with the environment",
                   "Наука о живых организмах, их строении, развитии и взаимодействии с окружающей средой"),
                "🧬", 4);

        findOrCreateSubject(owner,
                "Tarix", "Тарих", "History", "История",
                ml("O'tmishdagi voqealar, sivilizatsiyalar, davlatlar va jamiyatlarning rivojlanishini o'rganuvchi fan",
                   "Ўтмишдаги воқеалар, цивилизациялар, давлатлар ва жамиятларнинг ривожланишини ўрганувчи фан",
                   "The study of past events, civilizations, states and the development of societies",
                   "Изучение прошлых событий, цивилизаций, государств и развития обществ"),
                "📜", 5);

        findOrCreateSubject(owner,
                "Ona tili", "Она тили", "Uzbek Language", "Узбекский язык",
                ml("O'zbek tilining grammatikasi, adabiy me'yorlari, so'z boyligi va nutq madaniyatini o'rgatuvchi fan",
                   "Ўзбек тилининг грамматикаси, адабий меъёрлари, сўз бойлиги ва нутқ маданиятини ўргатувчи фан",
                   "Teaching Uzbek language grammar, literary norms, vocabulary and speech culture",
                   "Обучение грамматике узбекского языка, литературным нормам, словарному запасу и культуре речи"),
                "📝", 6);

        findOrCreateSubject(owner,
                "Ingliz tili", "Инглиз тили", "English", "Английский язык",
                ml("Ingliz tilini tinglash, gapirish, o'qish va yozish ko'nikmalari orqali chuqur o'rganish",
                   "Инглиз тилини тинглаш, гапириш, ўқиш ва ёзиш кўникмалари орқали чуқур ўрганиш",
                   "In-depth study of English through listening, speaking, reading and writing skills",
                   "Углублённое изучение английского языка через навыки аудирования, говорения, чтения и письма"),
                "🇬🇧", 7);

        findOrCreateSubject(owner,
                "Informatika", "Информатика", "Computer Science", "Информатика",
                ml("Dasturlash, algoritmlar, ma'lumotlar tuzilmasi va axborot texnologiyalarini o'rganuvchi fan",
                   "Дастурлаш, алгоритмлар, маълумотлар тузилмаси ва ахборот технологияларини ўрганувчи фан",
                   "The study of programming, algorithms, data structures and information technologies",
                   "Изучение программирования, алгоритмов, структур данных и информационных технологий"),
                "💻", 8);

        findOrCreateSubject(owner,
                "Geografiya", "География", "Geography", "География",
                ml("Yer yuzasi, iqlim, tabiiy resurslar, aholi va davlatlarning joylashishini o'rganuvchi fan",
                   "Ер юзаси, иқлим, табиий ресурслар, аҳоли ва давлатларнинг жойлашишини ўрганувчи фан",
                   "The study of Earth's surface, climate, natural resources, population and state locations",
                   "Изучение поверхности Земли, климата, природных ресурсов, населения и расположения государств"),
                "🌍", 9);

        findOrCreateSubject(owner,
                "Adabiyot", "Адабиёт", "Literature", "Литература",
                ml("Badiiy asarlarni tahlil qilish, adabiy janrlar, yozuvchilar va she'riyatni o'rganuvchi fan",
                   "Бадиий асарларни таҳлил қилиш, адабий жанрлар, ёзувчилар ва шеъриятни ўрганувчи фан",
                   "Analyzing literary works, literary genres, authors and poetry",
                   "Анализ художественных произведений, литературных жанров, авторов и поэзии"),
                "📚", 10);

        findOrCreateSubject(owner,
                "Rus tili", "Рус тили", "Russian Language", "Русский язык",
                ml("Rus tilining grammatikasi, leksikasi, fonetikasi va muloqot ko'nikmalarini o'rganuvchi fan",
                   "Рус тилининг грамматикаси, лексикаси, фонетикаси ва мулоқот кўникмаларини ўрганувчи фан",
                   "The study of Russian language grammar, lexicon, phonetics and communication skills",
                   "Изучение грамматики, лексики, фонетики русского языка и навыков общения"),
                "🇷🇺", 11);

        findOrCreateSubject(owner,
                "Huquq asoslari", "Ҳуқуқ асослари", "Fundamentals of Law", "Основы права",
                ml("Davlat va huquq, qonunchilik tizimi, fuqarolik va jinoyat huquqining asoslarini o'rganuvchi fan",
                   "Давлат ва ҳуқуқ, қонунчилик тизими, фуқаролик ва жиноят ҳуқуқининг асосларини ўрганувчи фан",
                   "The study of state and law, the legislative system, and the basics of civil and criminal law",
                   "Изучение государства и права, законодательной системы, основ гражданского и уголовного права"),
                "⚖️", 12);

        findOrCreateSubject(owner,
                "Iqtisodiyot", "Иқтисодиёт", "Economics", "Экономика",
                ml("Ishlab chiqarish, taqsimlash, iste'mol va bozor munosabatlarini o'rganuvchi ijtimoiy fan",
                   "Ишлаб чиқариш, тақсимлаш, истеъмол ва бозор муносабатларини ўрганувчи ижтимоий фан",
                   "A social science studying production, distribution, consumption and market relations",
                   "Общественная наука, изучающая производство, распределение, потребление и рыночные отношения"),
                "💰", 13);

        findOrCreateSubject(owner,
                "Falsafa", "Фалсафа", "Philosophy", "Философия",
                ml("Borliq, bilish, axloq, estetika va mantiq haqidagi fundamental savollarni o'rganuvchi fan",
                   "Борлиқ, билиш, ахлоқ, эстетика ва мантиқ ҳақидаги фундаментал саволларни ўрганувчи фан",
                   "The study of fundamental questions about existence, knowledge, ethics, aesthetics and logic",
                   "Изучение фундаментальных вопросов о бытии, познании, этике, эстетике и логике"),
                "🤔", 14);

        findOrCreateSubject(owner,
                "Psixologiya", "Психология", "Psychology", "Психология",
                ml("Inson xatti-harakati, his-tuyg'ulari, idrok va aqliy jarayonlarini o'rganuvchi fan",
                   "Инсон хатти-ҳаракати, ҳис-туйғулари, идрок ва ақлий жараёнларини ўрганувчи фан",
                   "The science of human behavior, emotions, perception and mental processes",
                   "Наука о поведении, эмоциях, восприятии и психических процессах человека"),
                "🧠", 15);

        log.info("Default subjects ensured (15 subjects with 4-language descriptions, owner: {})", owner.getEmail());
    }

    private void findOrCreateSubject(User owner,
                                      String uzLatn, String uzCyrl, String en, String ru,
                                      Map<String, String> description,
                                      String icon, int sortOrder) {
        Subject subject = subjectRepository.findByUserIdAndDefaultName(owner.getId(), uzLatn)
                .orElseGet(() -> {
                    Subject s = Subject.builder()
                            .user(owner)
                            .name(Map.of("uz_latn", uzLatn, "uz_cyrl", uzCyrl, "en", en, "ru", ru))
                            .description(description)
                            .icon(icon)
                            .isTemplate(true)
                            .isActive(true)
                            .isArchived(false)
                            .sortOrder(sortOrder)
                            .build();
                    s = subjectRepository.save(s);
                    log.info("Created subject: {} ({})", uzLatn, icon);
                    return s;
                });

        // Update description if it's missing (handles existing rows from previous deployments)
        if (subject.getDescription() == null || subject.getDescription().isEmpty()) {
            subject.setDescription(description);
            subjectRepository.save(subject);
            log.info("Updated description for subject: {}", uzLatn);
        }
    }

    private static Map<String, String> ml(String uzLatn, String uzCyrl, String en, String ru) {
        return Map.of("uz_latn", uzLatn, "uz_cyrl", uzCyrl, "en", en, "ru", ru);
    }
}
