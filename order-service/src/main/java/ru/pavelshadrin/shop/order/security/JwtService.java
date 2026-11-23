package ru.pavelshadrin.shop.order.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.pavelshadrin.shop.order.config.JwtProperties;
import ru.pavelshadrin.shop.order.dto.authentication.AuthTokenResponse;
import ru.pavelshadrin.shop.order.exception.InvalidAccessTokenException;
import ru.pavelshadrin.shop.order.exception.InvalidRefreshTokenException;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    @Autowired
    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtService(JwtProperties properties, Clock clock) {
        this.signingKey = createSigningKey(properties.getSecret());
        this.accessTokenTtl = requirePositive(properties.getAccessTokenTtl(), "access token TTL");
        this.refreshTokenTtl = requirePositive(properties.getRefreshTokenTtl(), "refresh token TTL");
        this.clock = clock;
    }

    public AuthTokenResponse issueTokenPair(UUID userId) {
        Instant issuedAt = Instant.now(clock);
        String accessToken = createToken(userId, ACCESS_TOKEN_TYPE, issuedAt, accessTokenTtl);
        String refreshToken = createToken(userId, REFRESH_TOKEN_TYPE, issuedAt, refreshTokenTtl);
        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                accessTokenTtl.toSeconds(),
                refreshTokenTtl.toSeconds()
        );
    }

    public UUID parseAccessToken(String token) {
        try {
            return parseSubject(token, ACCESS_TOKEN_TYPE);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidAccessTokenException(exception);
        }
    }

    public UUID parseRefreshToken(String token) {
        try {
            return parseSubject(token, REFRESH_TOKEN_TYPE);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidRefreshTokenException(exception);
        }
    }

    private String createToken(UUID userId, String tokenType, Instant issuedAt, Duration ttl) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(ttl)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    private UUID parseSubject(String token, String expectedTokenType) {
        Claims claims = Jwts.parser()
                .clock(() -> Date.from(Instant.now(clock)))
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String actualTokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedTokenType.equals(actualTokenType)) {
            throw new JwtException("Неверный тип JWT");
        }
        return UUID.fromString(claims.getSubject());
    }

    private SecretKey createSigningKey(String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET должен быть задан в Base64");
        }
        try {
            byte[] keyBytes = Decoders.BASE64.decode(encodedSecret);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET после Base64-декодирования должен содержать минимум 32 байта");
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET должен быть корректной Base64-строкой", exception);
        }
    }

    private Duration requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalStateException(name + " должен быть положительным");
        }
        return duration;
    }
}
