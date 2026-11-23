package ru.pavelshadrin.shop.order.exception;

public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException(Throwable cause) {
        super("Access token недействителен", cause);
    }
}
