package ru.pavelshadrin.shop.order.exception;

public class InventoryUnavailableException extends RuntimeException {

    public InventoryUnavailableException() {
        super("Inventory Service временно недоступен");
    }
}
