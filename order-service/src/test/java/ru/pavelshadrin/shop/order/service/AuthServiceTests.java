package ru.pavelshadrin.shop.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.pavelshadrin.shop.order.dto.authentication.LoginRequest;
import ru.pavelshadrin.shop.order.dto.authentication.RegisterRequest;
import ru.pavelshadrin.shop.order.dto.user.UserResponse;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.entity.UserRole;
import ru.pavelshadrin.shop.order.exception.InvalidCredentialsException;
import ru.pavelshadrin.shop.order.exception.UsernameAlreadyExistsException;
import ru.pavelshadrin.shop.order.mapper.UserMapper;
import ru.pavelshadrin.shop.order.repository.UserRepository;
import ru.pavelshadrin.shop.order.security.JwtService;
import ru.pavelshadrin.shop.order.security.OrderUserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private OrderUserDetailsService userDetailsService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                userDetailsService,
                new UserMapper()
        );
    }

    @Test
    void registerUserWithEncodedPasswordAndUserRole() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("pavel");
        request.setPassword("strong-password");
        request.setEmail("pavel@example.com");
        when(passwordEncoder.encode("strong-password")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("bcrypt-hash");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.USER);
        assertThat(response.getUsername()).isEqualTo("pavel");
        assertThat(response.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void rejectDuplicateUsernameBeforeSaving() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("pavel");
        request.setPassword("strong-password");
        request.setEmail("pavel@example.com");
        when(userRepository.existsByUsername("pavel")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void hideAuthenticationManagerFailureBehindInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("pavel");
        request.setPassword("wrong-password");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Неверное имя пользователя или пароль");
    }
}
