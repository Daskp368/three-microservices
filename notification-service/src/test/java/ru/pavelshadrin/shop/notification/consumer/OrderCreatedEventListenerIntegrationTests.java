package ru.pavelshadrin.shop.notification.consumer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import ru.pavelshadrin.shop.notification.support.PostgreSqlIntegrationTest;
import ru.pavelshadrin.shop.notification.repository.NotificationOrderRepository;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "app.kafka.listener-enabled=true",
        "spring.kafka.consumer.group-id=stage8-notification-integration"
})
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = "orders",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class OrderCreatedEventListenerIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private NotificationOrderRepository orderRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    @Test
    void consumeItemsRejectInvalidRecordsAndContinuePartition() throws Exception {
        UUID firstEventId = UUID.randomUUID();
        UUID firstOrderId = UUID.randomUUID();
        String firstPayload = twoItemPayload(firstEventId, firstOrderId, 1);

        send(firstOrderId, firstPayload);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(orderRepository.countByEventId(firstEventId)).isEqualTo(2)
        );

        UUID unsupportedEventId = UUID.randomUUID();
        UUID unsupportedOrderId = UUID.randomUUID();
        UUID wrongKeyEventId = UUID.randomUUID();
        UUID wrongKeyOrderId = UUID.randomUUID();
        UUID finalEventId = UUID.randomUUID();
        UUID finalOrderId = UUID.randomUUID();

        send(firstOrderId, firstPayload);
        send(unsupportedOrderId, oneItemPayload(unsupportedEventId, unsupportedOrderId, 99));
        send(UUID.randomUUID(), oneItemPayload(wrongKeyEventId, wrongKeyOrderId, 1));
        send(UUID.randomUUID(), "{not-json");
        send(finalOrderId, oneItemPayload(finalEventId, finalOrderId, 1));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(orderRepository.countByEventId(firstEventId)).isEqualTo(2);
            assertThat(orderRepository.countByEventId(unsupportedEventId)).isZero();
            assertThat(orderRepository.countByEventId(wrongKeyEventId)).isZero();
            assertThat(orderRepository.countByEventId(finalEventId)).isEqualTo(1);
            assertThat(orderRepository.count()).isEqualTo(3);
        });
    }

    private void send(UUID orderId, String payload) throws Exception {
        Map<String, Object> producerProperties = KafkaTestUtils.producerProps(embeddedKafka);
        try (Producer<String, String> producer = new KafkaProducer<>(
                producerProperties,
                new StringSerializer(),
                new StringSerializer()
        )) {
            producer.send(new ProducerRecord<>(
                    "orders",
                    orderId.toString(),
                    payload
            )).get();
        }
    }

    private String twoItemPayload(UUID eventId, UUID orderId, int version) {
        return """
                {
                  "eventId": "%s",
                  "eventVersion": %d,
                  "occurredAt": "2026-11-16T10:00:00Z",
                  "orderId": "%s",
                  "userId": "%s",
                  "totalPrice": 3539.70,
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": 2,
                      "price": 1200.00,
                      "sale": 15.00,
                      "totalPrice": 2040.00
                    },
                    {
                      "productId": "%s",
                      "quantity": 3,
                      "price": 499.90,
                      "sale": 0.00,
                      "totalPrice": 1499.70
                    }
                  ]
                }
                """.formatted(
                eventId,
                version,
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }

    private String oneItemPayload(UUID eventId, UUID orderId, int version) {
        return """
                {
                  "eventId": "%s",
                  "eventVersion": %d,
                  "occurredAt": "2026-11-16T10:00:00Z",
                  "orderId": "%s",
                  "userId": "%s",
                  "totalPrice": 10.00,
                  "items": [
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
                eventId,
                version,
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }
}
