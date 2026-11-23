package ru.pavelshadrin.shop.notification.event;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedItemEvent(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantity,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal price,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2) BigDecimal sale,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal totalPrice
) {
}
