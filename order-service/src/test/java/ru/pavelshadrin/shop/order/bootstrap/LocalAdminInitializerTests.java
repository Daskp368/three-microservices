package ru.pavelshadrin.shop.order.bootstrap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.pavelshadrin.shop.order.config.LocalAdminProperties;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.entity.UserRole;
import ru.pavelshadrin.shop.order.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalAdminInitializerTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments arguments;

    private LocalAdminProperties properties;
    private LocalAdminInitializer initializer;

    @BeforeEach
    void setUp() {
        properties = new LocalAdminProperties();
        initializer = new LocalAdminInitializer(properties, userRepository, passwordEncoder);
    }

    @Test
    void doNothingWhenBootstrapIsDisabled() {
        initializer.run(arguments);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void createAdminOnlyWhenExplicitlyEnabled() {
        enableAdmin();
        when(userRepository.findByUsername("local-admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin-password")).thenReturn("bcrypt-hash");

        initializer.run(arguments);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("local-admin");
        assertThat(captor.getValue().getEmail()).isEqualTo("local-admin@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("bcrypt-hash");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void keepExistingAdminWithoutResettingPassword() {
        enableAdmin();
        User existingAdmin = new User(
                "local-admin",
                "existing-hash",
                "local-admin@example.com",
                UserRole.ADMIN
        );
        when(userRepository.findByUsername("local-admin")).thenReturn(Optional.of(existingAdmin));

        initializer.run(arguments);

        verify(passwordEncoder, never()).encode("admin-password");
        verify(userRepository, never()).saveAndFlush(existingAdmin);
        assertThat(existingAdmin.getPassword()).isEqualTo("existing-hash");
    }

    private void enableAdmin() {
        properties.setEnabled(true);
        properties.setUsername("local-admin");
        properties.setEmail("local-admin@example.com");
        properties.setPassword("admin-password");
    }
}
