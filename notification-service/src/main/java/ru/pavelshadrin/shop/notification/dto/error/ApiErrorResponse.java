package ru.pavelshadrin.shop.notification.dto.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldValidationError> fieldErrors
) {
    public ApiErrorResponse {
        fieldErrors = List.copyOf(fieldErrors);
    }
}
