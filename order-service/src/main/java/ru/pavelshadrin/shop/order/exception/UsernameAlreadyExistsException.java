package ru.pavelshadrin.shop.order.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException() {
        super("Пользователь с таким именем уже существует");
    }
}
