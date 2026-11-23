package ru.pavelshadrin.shop.order.exception;

public class UserHasOrdersException extends RuntimeException {

    public UserHasOrdersException() {
        super("Нельзя удалить пользователя, у которого есть заказы");
    }
}
