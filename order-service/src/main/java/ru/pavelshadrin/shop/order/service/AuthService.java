package ru.pavelshadrin.shop.order.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pavelshadrin.shop.order.dto.authentication.AuthTokenResponse;
import ru.pavelshadrin.shop.order.dto.authentication.LoginRequest;
import ru.pavelshadrin.shop.order.dto.authentication.RefreshTokenRequest;
import ru.pavelshadrin.shop.order.dto.authentication.RegisterRequest;
import ru.pavelshadrin.shop.order.dto.user.UserResponse;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.entity.UserRole;
import ru.pavelshadrin.shop.order.exception.EmailAlreadyExistsException;
import ru.pavelshadrin.shop.order.exception.InvalidCredentialsException;
import ru.pavelshadrin.shop.order.exception.InvalidRefreshTokenException;
import ru.pavelshadrin.shop.order.exception.UsernameAlreadyExistsException;
import ru.pavelshadrin.shop.order.mapper.UserMapper;
import ru.pavelshadrin.shop.order.repository.UserRepository;
import ru.pavelshadrin.shop.order.security.JwtService;
import ru.pavelshadrin.shop.order.security.OrderUserDetailsService;
import ru.pavelshadrin.shop.order.security.UserPrincipal;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OrderUserDetailsService userDetailsService;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       JwtService jwtService, OrderUserDetailsService userDetailsService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException();
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getEmail(),
                UserRole.USER
        );
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
                throw new InvalidCredentialsException();
            }
            return jwtService.issueTokenPair(principal.getId());
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        UUID userId = jwtService.parseRefreshToken(request.getRefreshToken());
        try {
            userDetailsService.loadUserById(userId);
        } catch (UsernameNotFoundException exception) {
            throw new InvalidRefreshTokenException(exception);
        }
        return jwtService.issueTokenPair(userId);
    }
}
