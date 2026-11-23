package ru.pavelshadrin.shop.order.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaOutboxPublisherTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private OutboxMessageService outboxMessageService;

    @Test
    @SuppressWarnings("unchecked")
    void publishPendingMessageAndMarkItPublishedAfterKafkaAcknowledgement() throws Exception {
        OutboxMessage message = message();
        SendResult<String, String> sendResult = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(metadata.topic()).thenReturn("orders");
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(7L);
        when(kafkaTemplate.send(
                "orders",
                message.getAggregateId().toString(),
                message.getPayload().toString()
        )).thenReturn(CompletableFuture.completedFuture(sendResult));
        when(outboxMessageService.findPending()).thenReturn(List.of(message));
        KafkaOutboxPublisher publisher = publisher(Duration.ofSeconds(1));

        publisher.publishPending();

        ArgumentCaptor<Instant> publishedAt = ArgumentCaptor.forClass(Instant.class);
        verify(outboxMessageService).markPublished(eq(message.getId()), publishedAt.capture());
        assertThat(publishedAt.getValue()).isNotNull();
        verify(outboxMessageService, never()).markFailed(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void retainPendingMessageAndRecordErrorWhenKafkaRejectsSend() throws Exception {
        OutboxMessage message = message();
        CompletableFuture<SendResult<String, String>> failedSend = new CompletableFuture<>();
        failedSend.completeExceptionally(new KafkaException("broker unavailable"));
        when(kafkaTemplate.send(
                "orders",
                message.getAggregateId().toString(),
                message.getPayload().toString()
        )).thenReturn(failedSend);
        KafkaOutboxPublisher publisher = publisher(Duration.ofSeconds(1));

        boolean shouldContinue = publisher.publish(message);

        assertThat(shouldContinue).isTrue();
        verify(outboxMessageService).markFailed(
                message.getId(),
                "KafkaException: broker unavailable"
        );
        verify(outboxMessageService, never()).markPublished(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void retainPendingMessageWhenKafkaAcknowledgementTimesOut() throws Exception {
        OutboxMessage message = message();
        CompletableFuture<SendResult<String, String>> pendingSend = new CompletableFuture<>();
        when(kafkaTemplate.send(
                "orders",
                message.getAggregateId().toString(),
                message.getPayload().toString()
        )).thenReturn(pendingSend);
        KafkaOutboxPublisher publisher = publisher(Duration.ofMillis(1));

        boolean shouldContinue = publisher.publish(message);

        assertThat(shouldContinue).isTrue();
        verify(outboxMessageService).markFailed(
                message.getId(),
                "TimeoutException: no details"
        );
        verify(outboxMessageService, never()).markPublished(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void rejectNonPositiveSendTimeout() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> publisher(Duration.ZERO)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Outbox Kafka send timeout must be positive");
    }

    private KafkaOutboxPublisher publisher(Duration timeout) {
        return new KafkaOutboxPublisher(kafkaTemplate, outboxMessageService, timeout);
    }

    private OutboxMessage message() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        return new OutboxMessage(
                eventId,
                orderId,
                OBJECT_MAPPER.readTree("{\"eventId\":\"" + eventId + "\",\"eventVersion\":1}"),
                OutboxStatus.PENDING,
                0,
                Instant.now(),
                null,
                null
        );
    }
}
