package ru.pavelshadrin.shop.order.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super("Пользователь не найден: " + userId);
    }
}
