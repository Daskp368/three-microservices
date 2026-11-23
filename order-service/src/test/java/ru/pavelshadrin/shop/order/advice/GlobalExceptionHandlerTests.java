package ru.pavelshadrin.shop.order.advice;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import ru.pavelshadrin.shop.order.dto.error.ApiErrorResponse;
import ru.pavelshadrin.shop.order.exception.EmailAlreadyExistsException;
import ru.pavelshadrin.shop.order.exception.DuplicateProductException;
import ru.pavelshadrin.shop.order.exception.InvalidCredentialsException;
import ru.pavelshadrin.shop.order.exception.InvalidRefreshTokenException;
import ru.pavelshadrin.shop.order.exception.InsufficientStockException;
import ru.pavelshadrin.shop.order.exception.InventoryRequestValidationException;
import ru.pavelshadrin.shop.order.exception.InventoryTimeoutException;
import ru.pavelshadrin.shop.order.exception.InventoryUnavailableException;
import ru.pavelshadrin.shop.order.exception.ProductNotFoundException;
import ru.pavelshadrin.shop.order.exception.UserHasOrdersException;
import ru.pavelshadrin.shop.order.exception.UserNotFoundException;
import ru.pavelshadrin.shop.order.exception.UsernameAlreadyExistsException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapBusinessExceptionsToContractCodes() {
        assertError(
                handler.handleUserNotFound(new UserNotFoundException(UUID.randomUUID()), request("/api/users/id")),
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND"
        );
        assertError(
                handler.handleUsernameConflict(new UsernameAlreadyExistsException(), request("/auth/reg")),
                HttpStatus.CONFLICT,
                "USERNAME_ALREADY_EXISTS"
        );
        assertError(
                handler.handleEmailConflict(new EmailAlreadyExistsException(), request("/auth/reg")),
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_EXISTS"
        );
        assertError(
                handler.handleUserHasOrders(new UserHasOrdersException(), request("/api/users/id")),
                HttpStatus.CONFLICT,
                "USER_HAS_ORDERS"
        );
        assertError(
                handler.handleInvalidCredentials(new InvalidCredentialsException(), request("/auth/login")),
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS"
        );
        assertError(
                handler.handleInvalidRefreshToken(new InvalidRefreshTokenException(), request("/auth/refresh")),
                HttpStatus.UNAUTHORIZED,
                "INVALID_REFRESH_TOKEN"
        );
        UUID productId = UUID.randomUUID();
        assertError(
                handler.handleProductNotFound(new ProductNotFoundException(productId), request("/api/order")),
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND"
        );
        assertError(
                handler.handleInventoryValidation(new InventoryRequestValidationException(), request("/api/order")),
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED"
        );
        assertError(
                handler.handleDuplicateProduct(new DuplicateProductException(productId), request("/api/order")),
                HttpStatus.BAD_REQUEST,
                "DUPLICATE_PRODUCT"
        );
        assertError(
                handler.handleInsufficientStock(new InsufficientStockException(productId), request("/api/order")),
                HttpStatus.CONFLICT,
                "INSUFFICIENT_STOCK"
        );
        assertError(
                handler.handleInventoryUnavailable(new InventoryUnavailableException(), request("/api/order")),
                HttpStatus.SERVICE_UNAVAILABLE,
                "INVENTORY_UNAVAILABLE"
        );
        assertError(
                handler.handleInventoryTimeout(new InventoryTimeoutException(), request("/api/order")),
                HttpStatus.GATEWAY_TIMEOUT,
                "INVENTORY_TIMEOUT"
        );
    }

    private MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("POST", path);
    }

    private void assertError(ResponseEntity<ApiErrorResponse> response,
                             HttpStatus status, String code) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(status.value());
        assertThat(response.getBody().getCode()).isEqualTo(code);
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getFieldErrors()).isEmpty();
    }
}
