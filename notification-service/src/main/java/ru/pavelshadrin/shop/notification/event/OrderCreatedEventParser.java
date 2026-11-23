package ru.pavelshadrin.shop.notification.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrderCreatedEventParser {

    static final int SUPPORTED_EVENT_VERSION = 1;

    private final ObjectMapper kafkaObjectMapper;
    private final Validator validator;

    public OrderCreatedEventParser(ObjectMapper objectMapper, Validator validator) {
        this.kafkaObjectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.validator = validator;
    }

    public OrderCreatedEvent parse(String messageKey, String payload) {
        if (payload == null || payload.isBlank()) {
            throw new InvalidOrderCreatedEventException("Kafka event payload must not be blank");
        }

        OrderCreatedEvent event;
        try {
            event = kafkaObjectMapper.readValue(payload, OrderCreatedEvent.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new InvalidOrderCreatedEventException(
                    "Kafka event payload does not match OrderCreatedEvent JSON",
                    exception
            );
        }

        if (event == null) {
            throw new InvalidOrderCreatedEventException(
                    "Kafka event payload must contain an OrderCreatedEvent object"
            );
        }

        validateBeanConstraints(event);
        validateVersion(event);
        validateMessageKey(messageKey, event.orderId());
        validateUniqueProducts(event);
        validateOrderTotal(event);

        return event;
    }

    private void validateBeanConstraints(OrderCreatedEvent event) {
        Set<ConstraintViolation<OrderCreatedEvent>> violations = validator.validate(event);
        if (violations.isEmpty()) {
            return;
        }

        String details = violations.stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));

        throw new InvalidOrderCreatedEventException(
                "Kafka event validation failed: " + details
        );
    }

    private void validateVersion(OrderCreatedEvent event) {
        if (event.eventVersion() != SUPPORTED_EVENT_VERSION) {
            throw new InvalidOrderCreatedEventException(
                    "Unsupported OrderCreatedEvent version: " + event.eventVersion()
            );
        }
    }

    private void validateMessageKey(String messageKey, UUID orderId) {
        if (messageKey == null || !orderId.toString().equals(messageKey)) {
            throw new InvalidOrderCreatedEventException(
                    "Kafka message key must equal orderId"
            );
        }
    }

    private void validateUniqueProducts(OrderCreatedEvent event) {
        Set<UUID> productIds = new HashSet<>();
        boolean duplicateFound = event.items().stream()
                .map(OrderCreatedItemEvent::productId)
                .anyMatch(productId -> !productIds.add(productId));

        if (duplicateFound) {
            throw new InvalidOrderCreatedEventException(
                    "OrderCreatedEvent items must contain unique productId values"
            );
        }
    }

    private void validateOrderTotal(OrderCreatedEvent event) {
        BigDecimal itemTotal = event.items().stream()
                .map(OrderCreatedItemEvent::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (itemTotal.compareTo(event.totalPrice()) != 0) {
            throw new InvalidOrderCreatedEventException(
                    "OrderCreatedEvent totalPrice must equal the sum of item totalPrice values"
            );
        }
    }
}
