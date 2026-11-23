package ru.pavelshadrin.shop.order.service;

import org.springframework.stereotype.Component;
import ru.pavelshadrin.shop.order.client.ProductSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class OrderPriceCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;

    public CalculatedOrderItem calculate(ProductSnapshot product, int quantity) {
        BigDecimal discountedUnitPrice = product.price()
                .multiply(ONE_HUNDRED.subtract(product.sale()))
                .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalPrice = discountedUnitPrice
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return new CalculatedOrderItem(
                product,
                quantity,
                discountedUnitPrice,
                totalPrice
        );
    }
}
