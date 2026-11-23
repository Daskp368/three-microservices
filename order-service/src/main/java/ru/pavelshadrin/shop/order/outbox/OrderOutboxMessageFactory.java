package ru.pavelshadrin.shop.order.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ru.pavelshadrin.shop.order.entity.Order;
import ru.pavelshadrin.shop.order.event.OrderCreatedEvent;
import ru.pavelshadrin.shop.order.event.OrderCreatedItemEvent;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderOutboxMessageFactory {

    static final int EVENT_VERSION = 1;

    private final ObjectMapper objectMapper;

    public OrderOutboxMessageFactory(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OutboxMessage create(final Order order) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                EVENT_VERSION,
                occurredAt,
                order.getId(),
                order.getUser().getId(),
                order.getTotalPrice(),
                order.getItems().stream()
                        .map(item -> new OrderCreatedItemEvent(
                                item.getProductId(),
                                item.getQuantity(),
                                item.getPrice(),
                                item.getSale(),
                                item.getTotalPrice()
                        ))
                        .toList()
        );

        JsonNode payload;
        try {
            payload = objectMapper.valueToTree(event);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Не удалось сформировать JSON события созданного заказа",
                    exception
            );
        }

        if (!payload.isObject()) {
            throw new IllegalStateException(
                    "JSON события созданного заказа должен быть объектом"
            );
        }

        return new OutboxMessage(
                eventId,
                order.getId(),
                payload,
                OutboxStatus.PENDING,
                0,
                occurredAt,
                null,
                null
        );
    }
}
