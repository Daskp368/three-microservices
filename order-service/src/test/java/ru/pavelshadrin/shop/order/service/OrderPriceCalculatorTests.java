package ru.pavelshadrin.shop.order.service;

import org.junit.jupiter.api.Test;
import ru.pavelshadrin.shop.order.client.ProductSnapshot;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPriceCalculatorTests {

    private final OrderPriceCalculator calculator = new OrderPriceCalculator();

    @Test
    void calculatePriceWithoutSale() {
        CalculatedOrderItem result = calculator.calculate(
                product("499.90", "0.00"),
                3
        );

        assertThat(result.discountedUnitPrice()).isEqualByComparingTo("499.90");
        assertThat(result.totalPrice()).isEqualByComparingTo("1499.70");
    }

    @Test
    void applySaleAndRoundMoneyHalfUp() {
        CalculatedOrderItem result = calculator.calculate(
                product("10.01", "33.33"),
                2
        );

        assertThat(result.discountedUnitPrice()).isEqualByComparingTo("6.67");
        assertThat(result.totalPrice()).isEqualByComparingTo("13.34");
    }

    @Test
    void calculateZeroTotalForFullSale() {
        CalculatedOrderItem result = calculator.calculate(
                product("999.99", "100.00"),
                5
        );

        assertThat(result.discountedUnitPrice()).isEqualByComparingTo("0.00");
        assertThat(result.totalPrice()).isEqualByComparingTo("0.00");
        assertThat(result.totalPrice().scale()).isEqualTo(2);
    }

    private ProductSnapshot product(String price, String sale) {
        return new ProductSnapshot(
                UUID.randomUUID(),
                "Тестовый товар",
                10,
                new BigDecimal(price),
                new BigDecimal(sale),
                true
        );
    }
}
