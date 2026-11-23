package ru.pavelshadrin.shop.order.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.pavelshadrin.shop.order.config.LocalAdminProperties;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.entity.UserRole;
import ru.pavelshadrin.shop.order.repository.UserRepository;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class LocalAdminInitializer implements ApplicationRunner {

    private static final Pattern SIMPLE_EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final LocalAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalAdminInitializer(LocalAdminProperties properties,
                                 UserRepository userRepository,
                                 PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        validateConfiguration();

        Optional<User> existingByUsername = userRepository.findByUsername(properties.getUsername());
        if (existingByUsername.isPresent()) {
            User existingUser = existingByUsername.get();
            if (existingUser.getRole() != UserRole.ADMIN
                    || !existingUser.getEmail().equals(properties.getEmail())) {
                throw new IllegalStateException(
                        "LOCAL_ADMIN_USERNAME уже принадлежит другому пользователю"
                );
            }
            return;
        }

        if (userRepository.existsByEmail(properties.getEmail())) {
            throw new IllegalStateException("LOCAL_ADMIN_EMAIL уже используется другим пользователем");
        }

        User admin = new User(
                properties.getUsername(),
                passwordEncoder.encode(properties.getPassword()),
                properties.getEmail(),
                UserRole.ADMIN
        );
        userRepository.saveAndFlush(admin);
    }

    private void validateConfiguration() {
        String username = properties.getUsername();
        String email = properties.getEmail();
        String password = properties.getPassword();

        if (!StringUtils.hasText(username) || username.length() < 3 || username.length() > 50) {
            throw new IllegalStateException("LOCAL_ADMIN_USERNAME должен содержать от 3 до 50 символов");
        }
        if (!StringUtils.hasText(email) || email.length() > 254
                || !SIMPLE_EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalStateException("LOCAL_ADMIN_EMAIL имеет некорректный формат");
        }
        if (!StringUtils.hasText(password) || password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException("LOCAL_ADMIN_PASSWORD должен содержать от 8 до 72 символов");
        }
    }
}
