package ru.pavelshadrin.shop.order.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("Пользователь с таким email уже существует");
    }
}
