package ru.pavelshadrin.shop.order.outbox;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnProperty(
        name = "app.outbox.publisher-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KafkaOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxPublisher.class);

    private static final String ORDERS_TOPIC = "orders";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxMessageService outboxMessageService;
    private final Duration sendTimeout;

    public KafkaOutboxPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            OutboxMessageService outboxMessageService,
            @Value("${app.outbox.send-timeout}") Duration sendTimeout
    ) {
        if (sendTimeout.isZero() || sendTimeout.isNegative()) {
            throw new IllegalArgumentException("Outbox Kafka send timeout must be positive");
        }

        this.kafkaTemplate = kafkaTemplate;
        this.outboxMessageService = outboxMessageService;
        this.sendTimeout = sendTimeout;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher-delay}")
    public void publishPending() {
        for (OutboxMessage message : outboxMessageService.findPending()) {
            boolean shouldContinue = publish(message);

            if (!shouldContinue) {
                return;
            }
        }
    }

    boolean publish(OutboxMessage message) {
        SendResult<String, String> result;

        try {
            result = kafkaTemplate.send(
                    ORDERS_TOPIC,
                    message.getAggregateId().toString(),
                    message.getPayload().toString()
            ).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            recordFailure(message, exception);
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            recordFailure(message, exception);
            return true;
        }

        Instant publishedAt = Instant.now();

        try {
            outboxMessageService.markPublished(
                    message.getId(),
                    publishedAt
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Kafka accepted event, but outbox status update failed: eventId={}, orderId={}",
                    message.getId(),
                    message.getAggregateId(),
                    exception
            );
            return true;
        }

        RecordMetadata metadata = result.getRecordMetadata();

        log.info(
                "Order event published: eventId={}, orderId={}, topic={}, partition={}, offset={}",
                message.getId(),
                message.getAggregateId(),
                metadata.topic(),
                metadata.partition(),
                metadata.offset()
        );

        return true;
    }

    private void recordFailure(OutboxMessage message, Exception exception) {
        Throwable cause = exception;
        if (exception instanceof ExecutionException
                && exception.getCause() != null) {
            cause = exception.getCause();
        }

        String error = cause.getClass().getSimpleName()
                + ": "
                + (cause.getMessage() == null ? "no details" : cause.getMessage());

        try {
            outboxMessageService.markFailed(message.getId(), error);
        } catch (RuntimeException statusException) {
            log.error(
                    "Kafka publication failed and outbox status update failed: eventId={}, orderId={}",
                    message.getId(),
                    message.getAggregateId(),
                    statusException
            );
        }

        log.warn(
                "Order event publication failed; message remains pending: eventId={}, orderId={}, error={}",
                message.getId(),
                message.getAggregateId(),
                error
        );
    }
}
