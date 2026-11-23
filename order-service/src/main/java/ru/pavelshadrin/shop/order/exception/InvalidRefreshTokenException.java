package ru.pavelshadrin.shop.order.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token недействителен");
    }

    public InvalidRefreshTokenException(Throwable cause) {
        super("Refresh token недействителен", cause);
    }
}
