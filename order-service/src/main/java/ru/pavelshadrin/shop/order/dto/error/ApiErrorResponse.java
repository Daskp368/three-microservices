package ru.pavelshadrin.shop.order.dto.error;

import java.time.Instant;
import java.util.List;

public class ApiErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String code;
    private final String message;
    private final String path;
    private final List<FieldValidationError> fieldErrors;

    public ApiErrorResponse(Instant timestamp, int status, String code, String message,
                            String path, List<FieldValidationError> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<FieldValidationError> getFieldErrors() {
        return fieldErrors;
    }
}
