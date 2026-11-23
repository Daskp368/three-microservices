package ru.pavelshadrin.shop.order.exception;

public class InventoryTimeoutException extends RuntimeException {

    public InventoryTimeoutException() {
        super("Превышено время ожидания ответа Inventory Service");
    }
}
