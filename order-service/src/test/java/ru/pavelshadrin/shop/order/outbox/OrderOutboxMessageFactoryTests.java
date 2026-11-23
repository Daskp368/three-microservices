package ru.pavelshadrin.shop.order.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.pavelshadrin.shop.order.entity.Order;
import ru.pavelshadrin.shop.order.entity.OrderItem;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.entity.UserRole;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderOutboxMessageFactoryTests {

    private final OrderOutboxMessageFactory factory = new OrderOutboxMessageFactory(
            new ObjectMapper().findAndRegisterModules()
    );

    @Test
    void createVersionedEventFromSavedOrder() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        User user = new User("event-user", "hash", "event-user@example.com", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", userId);

        Order order = new Order(user, new BigDecimal("2040.00"));
        ReflectionTestUtils.setField(order, "id", orderId);
        order.addItem(new OrderItem(
                productId,
                "Товар",
                2,
                new BigDecimal("1200.00"),
                new BigDecimal("15.00"),
                new BigDecimal("1020.00"),
                new BigDecimal("2040.00")
        ));

        OutboxMessage message = factory.create(order);
        JsonNode payload = message.getPayload();

        assertThat(message.getId()).isNotNull();
        assertThat(message.getAggregateId()).isEqualTo(orderId);
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.getAttempts()).isZero();
        assertThat(message.getCreatedAt()).isNotNull();
        assertThat(message.getPublishedAt()).isNull();
        assertThat(message.getLastError()).isNull();

        assertThat(payload.get("eventId").asText()).isEqualTo(message.getId().toString());
        assertThat(payload.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(payload.get("occurredAt").asText()).isNotBlank();
        assertThat(payload.get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(payload.get("userId").asText()).isEqualTo(userId.toString());
        assertThat(payload.get("totalPrice").decimalValue()).isEqualByComparingTo("2040.00");
        assertThat(payload.get("items")).singleElement().satisfies(item -> {
            assertThat(item.get("productId").asText()).isEqualTo(productId.toString());
            assertThat(item.get("quantity").asInt()).isEqualTo(2);
            assertThat(item.get("price").decimalValue()).isEqualByComparingTo("1200.00");
            assertThat(item.get("sale").decimalValue()).isEqualByComparingTo("15.00");
            assertThat(item.get("totalPrice").decimalValue()).isEqualByComparingTo("2040.00");
            assertThat(item.has("productName")).isFalse();
            assertThat(item.has("discountedUnitPrice")).isFalse();
        });
        assertThat(payload.has("status")).isFalse();
    }
}
