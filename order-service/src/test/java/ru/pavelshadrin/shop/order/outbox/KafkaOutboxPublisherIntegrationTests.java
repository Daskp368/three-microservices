package ru.pavelshadrin.shop.order.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import ru.pavelshadrin.shop.order.support.PostgreSqlIntegrationTest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.outbox.publisher-enabled=true",
        "app.outbox.publisher-delay=1h",
        "app.outbox.send-timeout=5s"
})
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = "orders",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class KafkaOutboxPublisherIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private KafkaOutboxPublisher publisher;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @BeforeEach
    void cleanOutbox() {
        outboxMessageRepository.deleteAll();
    }

    @Test
    void publishJsonWithOrderIdKeyToOrdersTopicAndMarkMessagePublished() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        JsonNode payload = objectMapper.readTree("""
                {
                  "eventId": "%s",
                  "eventVersion": 1,
                  "occurredAt": "2026-11-14T10:00:00Z",
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
                """.formatted(eventId, orderId, UUID.randomUUID(), productId));
        OutboxMessage message = outboxMessageRepository.saveAndFlush(new OutboxMessage(
                eventId,
                orderId,
                payload,
                OutboxStatus.PENDING,
                0,
                Instant.now(),
                null,
                null
        ));

        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(
                "stage7-publisher-" + UUID.randomUUID(),
                "true",
                embeddedKafka
        );
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProperties,
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "orders");

            assertThat(publisher.publish(message)).isTrue();

            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
                    consumer,
                    "orders",
                    Duration.ofSeconds(10)
            );
            assertThat(record.key()).isEqualTo(orderId.toString());
            assertThat(objectMapper.readTree(record.value())).isEqualTo(payload);
        }

        OutboxMessage published = outboxMessageRepository.findById(eventId).orElseThrow();
        assertThat(published.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(published.getAttempts()).isEqualTo(1);
        assertThat(published.getPublishedAt()).isNotNull();
        assertThat(published.getLastError()).isNull();
    }
}
