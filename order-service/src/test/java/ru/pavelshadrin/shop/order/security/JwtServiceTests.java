package ru.pavelshadrin.shop.order.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pavelshadrin.shop.order.config.JwtProperties;
import ru.pavelshadrin.shop.order.dto.authentication.AuthTokenResponse;
import ru.pavelshadrin.shop.order.exception.InvalidAccessTokenException;
import ru.pavelshadrin.shop.order.exception.InvalidRefreshTokenException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTests {

    private static final String JWT_SECRET =
            "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-05T10:00:00Z"), ZoneOffset.UTC);
        jwtService = createJwtService(clock);
    }

    @Test
    void issueAndParseTokenPair() {
        UUID userId = UUID.randomUUID();

        AuthTokenResponse response = jwtService.issueTokenPair(userId);

        assertThat(jwtService.parseAccessToken(response.getAccessToken())).isEqualTo(userId);
        assertThat(jwtService.parseRefreshToken(response.getRefreshToken())).isEqualTo(userId);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getAccessExpiresIn()).isEqualTo(900);
        assertThat(response.getRefreshExpiresIn()).isEqualTo(604800);
    }

    @Test
    void rejectAccessTokenAsRefreshToken() {
        AuthTokenResponse response = jwtService.issueTokenPair(UUID.randomUUID());

        assertThatThrownBy(() -> jwtService.parseRefreshToken(response.getAccessToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectRefreshTokenAsAccessToken() {
        AuthTokenResponse response = jwtService.issueTokenPair(UUID.randomUUID());

        assertThatThrownBy(() -> jwtService.parseAccessToken(response.getRefreshToken()))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void rejectTokensAfterTheirOwnExpirationTime() {
        Instant issuedAt = Instant.parse("2026-11-05T10:00:00Z");
        AuthTokenResponse response = createJwtService(fixedClock(issuedAt))
                .issueTokenPair(UUID.randomUUID());

        JwtService afterAccessExpiration = createJwtService(
                fixedClock(issuedAt.plus(Duration.ofMinutes(16)))
        );
        assertThatThrownBy(() -> afterAccessExpiration.parseAccessToken(response.getAccessToken()))
                .isInstanceOf(InvalidAccessTokenException.class);
        assertThat(afterAccessExpiration.parseRefreshToken(response.getRefreshToken()))
                .isNotNull();

        JwtService afterRefreshExpiration = createJwtService(
                fixedClock(issuedAt.plus(Duration.ofDays(8)))
        );
        assertThatThrownBy(() -> afterRefreshExpiration.parseRefreshToken(response.getRefreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    private JwtService createJwtService(Clock clock) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(JWT_SECRET);
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        return new JwtService(properties, clock);
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
