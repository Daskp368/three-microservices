package ru.pavelshadrin.shop.order.exception;

public class InventoryRequestValidationException extends RuntimeException {

    public InventoryRequestValidationException() {
        super("Inventory Service отклонил параметры запроса");
    }
}
