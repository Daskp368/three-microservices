package ru.pavelshadrin.shop.inventory.dto.product;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductRequest {

    @NotBlank(message = "Название товара обязательно")
    @Size(max = 255, message = "Название товара должно содержать не более 255 символов")
    private String name;

    @NotNull(message = "Количество товара обязательно")
    @PositiveOrZero(message = "Количество товара не может быть отрицательным")
    private Integer quantity;

    @NotNull(message = "Цена товара обязательна")
    @DecimalMin(value = "0.00", message = "Цена товара не может быть отрицательной")
    @Digits(integer = 17, fraction = 2, message = "Цена может содержать до 17 целых и 2 дробных цифр")
    private BigDecimal price;

    @NotNull(message = "Скидка обязательна")
    @DecimalMin(value = "0.00", message = "Скидка не может быть отрицательной")
    @DecimalMax(value = "100.00", message = "Скидка не может быть больше 100")
    @Digits(integer = 3, fraction = 2, message = "Скидка может содержать до 3 целых и 2 дробных цифр")
    private BigDecimal sale;

    public ProductRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSale() {
        return sale;
    }

    public void setSale(BigDecimal sale) {
        this.sale = sale;
    }
}
