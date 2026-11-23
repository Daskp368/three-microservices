package ru.pavelshadrin.shop.order.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.inventory.grpc")
public class InventoryGrpcProperties {

    @NotBlank(message = "Inventory gRPC host обязателен")
    private String host;

    @Min(value = 1, message = "Inventory gRPC port должен быть больше нуля")
    @Max(value = 65535, message = "Inventory gRPC port должен быть не больше 65535")
    private int port;

    @NotNull(message = "Inventory gRPC deadline обязателен")
    private Duration deadline;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Duration getDeadline() {
        return deadline;
    }

    public void setDeadline(Duration deadline) {
        this.deadline = deadline;
    }

    @AssertTrue(message = "Inventory gRPC deadline должен быть положительным")
    public boolean isDeadlinePositive() {
        return deadline != null
                && !deadline.isZero()
                && !deadline.isNegative();
    }
}
