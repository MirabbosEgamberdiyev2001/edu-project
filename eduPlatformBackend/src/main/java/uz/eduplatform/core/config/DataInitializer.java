package uz.eduplatform.core.config;

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

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail("superadmin@eduplatform.uz")) {
            log.info("Default users already exist, skipping initialization");
            return;
        }

        String encodedPassword = passwordEncoder.encode("Password1");

        List<User> defaultUsers = List.of(
                buildUser("superadmin@eduplatform.uz", "Super", "Admin", Role.SUPER_ADMIN, encodedPassword),
                buildUser("admin@eduplatform.uz", "Admin", "User", Role.ADMIN, encodedPassword),
                buildUser("moderator@eduplatform.uz", "Moderator", "User", Role.MODERATOR, encodedPassword),
                buildUser("teacher@eduplatform.uz", "Teacher", "User", Role.TEACHER, encodedPassword),
                buildUser("parent@eduplatform.uz", "Parent", "User", Role.PARENT, encodedPassword),
                buildUser("student@eduplatform.uz", "Student", "User", Role.STUDENT, encodedPassword)
        );

        userRepository.saveAll(defaultUsers);
        log.info("Created {} default users for dev environment", defaultUsers.size());
    }

    private User buildUser(String email, String firstName, String lastName, Role role, String encodedPassword) {
        return User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .passwordHash(encodedPassword)
                .passwordChangedAt(LocalDateTime.now())
                .build();
    }
}
