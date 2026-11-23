package ru.pavelshadrin.shop.order.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import ru.pavelshadrin.shop.order.dto.authentication.AuthTokenResponse;
import ru.pavelshadrin.shop.order.dto.authentication.LoginRequest;
import ru.pavelshadrin.shop.order.dto.authentication.RefreshTokenRequest;
import ru.pavelshadrin.shop.order.dto.authentication.RegisterRequest;
import ru.pavelshadrin.shop.order.dto.user.UpdateUserRequest;
import ru.pavelshadrin.shop.order.dto.user.UserResponse;
import ru.pavelshadrin.shop.order.entity.UserRole;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthAndUserDtoValidationTests {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();

    private final Validator validator = VALIDATOR_FACTORY.getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void acceptValidRegisterRequest() {
        RegisterRequest request = validRegisterRequest();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectInvalidRegisterFields() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ab");
        request.setPassword("short");
        request.setEmail("incorrect-email");

        Set<String> invalidFields = validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields).containsExactlyInAnyOrder("username", "password", "email");
    }

    @Test
    void rejectPasswordLongerThanBcryptLimit() {
        RegisterRequest request = validRegisterRequest();
        request.setPassword("x".repeat(73));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("password");
    }

    @Test
    void rejectBlankLoginAndRefreshToken() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(" ");
        loginRequest.setPassword("");

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken(" ");

        assertThat(validator.validate(loginRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("username", "password");
        assertThat(validator.validate(refreshTokenRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("refreshToken");
    }

    @Test
    void requireRoleWhenUpdatingUser() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("updated-user");
        request.setEmail("updated-user@example.com");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("role");
    }

    @Test
    void serializeUserResponseWithoutPassword() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse(
                userId,
                "pavel",
                "pavel@example.com",
                UserRole.USER
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("id").asText()).isEqualTo(userId.toString());
        assertThat(json.get("username").asText()).isEqualTo("pavel");
        assertThat(json.get("email").asText()).isEqualTo("pavel@example.com");
        assertThat(json.get("role").asText()).isEqualTo("USER");
        assertThat(json.has("password")).isFalse();
    }

    @Test
    void serializeTokenResponseUsingContractFieldNames() throws Exception {
        AuthTokenResponse response = new AuthTokenResponse(
                "access-token",
                "refresh-token",
                900,
                604800
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("accessToken").asText()).isEqualTo("access-token");
        assertThat(json.get("refreshToken").asText()).isEqualTo("refresh-token");
        assertThat(json.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(json.get("accessExpiresIn").asLong()).isEqualTo(900);
        assertThat(json.get("refreshExpiresIn").asLong()).isEqualTo(604800);
    }

    @Test
    void rejectUnknownRegisterField() {
        String json = """
                {
                  "username": "pavel",
                  "password": "strong-password",
                  "email": "pavel@example.com",
                  "role": "ADMIN"
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, RegisterRequest.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }

    private RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("pavel");
        request.setPassword("strong-password");
        request.setEmail("pavel@example.com");
        return request;
    }
}
