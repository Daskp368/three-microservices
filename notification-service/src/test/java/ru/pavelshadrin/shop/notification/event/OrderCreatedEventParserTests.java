package ru.pavelshadrin.shop.notification.event;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderCreatedEventParserTests {

    private final ObjectMapper strictObjectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final OrderCreatedEventParser parser = new OrderCreatedEventParser(
            strictObjectMapper,
            Validation.buildDefaultValidatorFactory().getValidator()
    );

    @Test
    void parseVersionOneAndIgnoreNewOptionalFields() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderCreatedEvent event = parser.parse(orderId.toString(), """
                {
                  "eventId": "%s",
                  "eventVersion": 1,
                  "occurredAt": "2026-11-16T10:00:00Z",
                  "orderId": "%s",
                  "userId": "%s",
                  "totalPrice": 2040.00,
                  "futureTopLevelField": "ignored",
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": 2,
                      "price": 1200.00,
                      "sale": 15.00,
                      "totalPrice": 2040.00,
                      "futureItemField": true
                    }
                  ]
                }
                """.formatted(eventId, orderId, userId, productId));

        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.eventVersion()).isEqualTo(1);
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.totalPrice()).isEqualByComparingTo("2040.00");
        assertThat(event.items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(productId);
            assertThat(item.quantity()).isEqualTo(2);
            assertThat(item.price()).isEqualByComparingTo("1200.00");
            assertThat(item.sale()).isEqualByComparingTo("15.00");
            assertThat(item.totalPrice()).isEqualByComparingTo("2040.00");
        });
    }

    @Test
    void rejectUnsupportedVersion() {
        UUID orderId = UUID.randomUUID();

        assertThatThrownBy(() -> parser.parse(orderId.toString(), validPayload(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                2
        )))
                .isInstanceOf(InvalidOrderCreatedEventException.class)
                .hasMessage("Unsupported OrderCreatedEvent version: 2");
    }

    @Test
    void rejectJsonNullInsteadOfEventObject() {
        assertThatThrownBy(() -> parser.parse(UUID.randomUUID().toString(), "null"))
                .isInstanceOf(InvalidOrderCreatedEventException.class)
                .hasMessage("Kafka event payload must contain an OrderCreatedEvent object");
    }

    @Test
    void rejectMessageKeyThatDoesNotEqualOrderId() {
        assertThatThrownBy(() -> parser.parse(UUID.randomUUID().toString(), validPayload(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1
        )))
                .isInstanceOf(InvalidOrderCreatedEventException.class)
                .hasMessage("Kafka message key must equal orderId");
    }

    @Test
    void rejectDuplicateProductInsideEvent() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        String payload = """
                {
                  "eventId": "%s",
                  "eventVersion": 1,
                  "occurredAt": "2026-11-16T10:00:00Z",
                  "orderId": "%s",
                  "userId": "%s",
                  "totalPrice": 20.00,
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": 1,
                      "price": 10.00,
                      "sale": 0.00,
                      "totalPrice": 10.00
                    },
                    {
                      "productId": "%s",
                      "quantity": 1,
                      "price": 10.00,
                      "sale": 0.00,
                      "totalPrice": 10.00
                    }
                  ]
                }
                """.formatted(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                productId,
                productId
        );

        assertThatThrownBy(() -> parser.parse(orderId.toString(), payload))
                .isInstanceOf(InvalidOrderCreatedEventException.class)
                .hasMessage("OrderCreatedEvent items must contain unique productId values");
    }

    @Test
    void rejectOrderTotalThatDoesNotEqualItemTotals() {
        UUID orderId = UUID.randomUUID();
        String payload = validPayload(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                1
        ).replace("2040.00,\n  \"items\"", "999.00,\n  \"items\"");

        assertThatThrownBy(() -> parser.parse(orderId.toString(), payload))
                .isInstanceOf(InvalidOrderCreatedEventException.class)
                .hasMessage("OrderCreatedEvent totalPrice must equal the sum of item totalPrice values");
    }

    @Test
    void rejectMissingRequiredFieldAndInvalidQuantity() {
        UUID orderId = UUID.randomUUID();
        String payload = """
                {
                  "eventId": "%s",
                  "eventVersion": 1,
                  "occurredAt": "2026-11-16T10:00:00Z",
                  "orderId": "%s",
                  "userId": "%s",
                  "totalPrice": 0.00,
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": 0,
                      "sale": 0.00,
                      "totalPrice": 0.00
                    }
                  ]
                }
                """.formatted(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        assertThatThrownBy(() -> parser.parse(orderId.toString(), payload))
                .isInstanceOf(InvalidOrderCreatedEventException.class)
                .hasMessageContaining("items[0].price")
                .hasMessageContaining("items[0].quantity");
    }

    private String validPayload(
            UUID eventId,
            UUID orderId,
            UUID userId,
            UUID productId,
            int eventVersion
    ) {
        return """
                {
                  "eventId": "%s",
                  "eventVersion": %d,
                  "occurredAt": "2026-11-16T10:00:00Z",
                  "orderId": "%s",
                  "userId": "%s",
                  "totalPrice": 2040.00,
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": 2,
                      "price": 1200.00,
                      "sale": 15.00,
                      "totalPrice": 2040.00
                    }
                  ]
                }
                """.formatted(eventId, eventVersion, orderId, userId, productId);
    }
}
