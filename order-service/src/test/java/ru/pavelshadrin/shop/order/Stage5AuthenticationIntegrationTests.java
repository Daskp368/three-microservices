package ru.pavelshadrin.shop.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import ru.pavelshadrin.shop.order.support.PostgreSqlIntegrationTest;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.entity.UserRole;
import ru.pavelshadrin.shop.order.repository.OrderRepository;
import ru.pavelshadrin.shop.order.repository.UserRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Stage5AuthenticationIntegrationTests extends PostgreSqlIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerLoginAndRejectAccessTokenAtRefreshEndpoint() throws Exception {
        ResponseEntity<String> registration = post("/auth/reg", Map.of(
                "username", "stage-user",
                "password", "strong-password",
                "email", "stage-user@example.com"
        ));
        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registration.getBody()).doesNotContain("password");

        ResponseEntity<String> login = post("/auth/login", Map.of(
                "username", "stage-user",
                "password", "strong-password"
        ));
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode tokens = objectMapper.readTree(login.getBody());

        ResponseEntity<String> refreshWithAccessToken = post("/auth/refresh", Map.of(
                "refreshToken", tokens.get("accessToken").asText()
        ));
        assertThat(refreshWithAccessToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refreshWithAccessToken.getBody()).contains("INVALID_REFRESH_TOKEN");

        ResponseEntity<String> refresh = post("/auth/refresh", Map.of(
                "refreshToken", tokens.get("refreshToken").asText()
        ));
        assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refresh.getBody()).contains("accessToken", "refreshToken", "Bearer");
    }

    @Test
    void enforceAdminAccessAndCompleteUserCrud() throws Exception {
        userRepository.saveAndFlush(new User(
                "stage-admin",
                passwordEncoder.encode("admin-password"),
                "stage-admin@example.com",
                UserRole.ADMIN
        ));

        ResponseEntity<String> registration = post("/auth/reg", Map.of(
                "username", "managed-user",
                "password", "strong-password",
                "email", "managed-user@example.com"
        ));
        String userId = objectMapper.readTree(registration.getBody()).get("id").asText();

        String adminToken = loginAndGetAccessToken("stage-admin", "admin-password");
        String userToken = loginAndGetAccessToken("managed-user", "strong-password");

        assertThat(get("/api/users", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/users", userToken).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/users", adminToken).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> updated = exchange(
                "/api/users/" + userId,
                HttpMethod.PUT,
                Map.of(
                        "username", "managed-user-updated",
                        "email", "managed-user-updated@example.com",
                        "role", "ADMIN"
                ),
                adminToken
        );
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).contains("managed-user-updated", "ADMIN");

        ResponseEntity<String> deleted = exchange(
                "/api/users/" + userId,
                HttpMethod.DELETE,
                null,
                adminToken
        );
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> missing = get("/api/users/" + userId, adminToken);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).contains("USER_NOT_FOUND");
    }

    @Test
    void rejectDuplicateRegistrationAndUnknownFields() {
        post("/auth/reg", Map.of(
                "username", "duplicate-user",
                "password", "strong-password",
                "email", "duplicate-user@example.com"
        ));

        ResponseEntity<String> duplicate = post("/auth/reg", Map.of(
                "username", "duplicate-user",
                "password", "another-password",
                "email", "other@example.com"
        ));
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody()).contains("USERNAME_ALREADY_EXISTS");

        ResponseEntity<String> unknownRole = post("/auth/reg", Map.of(
                "username", "unexpected-role",
                "password", "strong-password",
                "email", "unexpected-role@example.com",
                "role", "ADMIN"
        ));
        assertThat(unknownRole.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unknownRole.getBody()).contains("VALIDATION_FAILED");
    }

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        ResponseEntity<String> response = post("/auth/login", Map.of(
                "username", username,
                "password", password
        ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody()).get("accessToken").asText();
    }

    private ResponseEntity<String> post(String path, Object body) {
        return exchange(path, HttpMethod.POST, body, null);
    }

    private ResponseEntity<String> get(String path, String token) {
        return exchange(path, HttpMethod.GET, null, token);
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method,
                                            Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(
                "http://localhost:" + port + path,
                method,
                new HttpEntity<>(body, headers),
                String.class
        );
    }
}
