package ru.pavelshadrin.shop.order.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Неверное имя пользователя или пароль");
    }
}
