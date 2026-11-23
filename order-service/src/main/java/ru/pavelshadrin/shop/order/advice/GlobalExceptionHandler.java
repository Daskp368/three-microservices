package ru.pavelshadrin.shop.order.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.pavelshadrin.shop.order.dto.error.ApiErrorResponse;
import ru.pavelshadrin.shop.order.dto.error.FieldValidationError;
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

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
            UserNotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameConflict(
            UsernameAlreadyExistsException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailConflict(
            EmailAlreadyExistsException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(UserHasOrdersException.class)
    public ResponseEntity<ApiErrorResponse> handleUserHasOrders(
            UserHasOrdersException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "USER_HAS_ORDERS",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound(
            ProductNotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(InventoryRequestValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleInventoryValidation(
            InventoryRequestValidationException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicateProductException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateProduct(
            DuplicateProductException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "DUPLICATE_PRODUCT",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientStock(
            InsufficientStockException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(InventoryUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleInventoryUnavailable(
            InventoryUnavailableException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "INVENTORY_UNAVAILABLE",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(InventoryTimeoutException.class)
    public ResponseEntity<ApiErrorResponse> handleInventoryTimeout(
            InventoryTimeoutException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.GATEWAY_TIMEOUT, "INVENTORY_TIMEOUT",
                exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldValidationError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldValidationError::getField))
                .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Запрос не прошел валидацию", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Некорректное тело запроса", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidId(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_ID",
                "Идентификатор должен иметь формат UUID", request, List.of());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<FieldValidationError> fieldErrors) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(response);
    }
}
