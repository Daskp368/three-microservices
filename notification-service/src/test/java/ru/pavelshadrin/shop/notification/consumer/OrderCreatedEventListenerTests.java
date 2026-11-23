package ru.pavelshadrin.shop.notification.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.pavelshadrin.shop.notification.event.InvalidOrderCreatedEventException;
import ru.pavelshadrin.shop.notification.event.OrderCreatedEvent;
import ru.pavelshadrin.shop.notification.event.OrderCreatedEventParser;
import ru.pavelshadrin.shop.notification.service.NotificationOrderService;
import ru.pavelshadrin.shop.notification.service.OrderEventSaveResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreatedEventListenerTests {

    @Mock
    private OrderCreatedEventParser eventParser;

    @Mock
    private NotificationOrderService orderService;

    private OrderCreatedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderCreatedEventListener(eventParser, orderService);
    }

    @Test
    void saveValidEvent() {
        ConsumerRecord<String, String> record = record();
        OrderCreatedEvent event = event();
        when(eventParser.parse(record.key(), record.value())).thenReturn(event);
        when(orderService.save(event)).thenReturn(OrderEventSaveResult.SAVED);

        listener.onOrderCreated(record);

        verify(orderService).save(event);
    }

    @Test
    void treatInvalidContractAsHandledWithoutSaving() {
        ConsumerRecord<String, String> record = record();
        when(eventParser.parse(record.key(), record.value()))
                .thenThrow(new InvalidOrderCreatedEventException("unsupported version"));

        listener.onOrderCreated(record);

        verify(orderService, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void propagatePersistenceFailureForKafkaRedelivery() {
        ConsumerRecord<String, String> record = record();
        OrderCreatedEvent event = event();
        when(eventParser.parse(record.key(), record.value())).thenReturn(event);
        doThrow(new IllegalStateException("database unavailable"))
                .when(orderService).save(event);

        assertThatThrownBy(() -> listener.onOrderCreated(record))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }

    private ConsumerRecord<String, String> record() {
        return new ConsumerRecord<>("orders", 0, 42L, UUID.randomUUID().toString(), "{}");
    }

    private OrderCreatedEvent event() {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                1,
                Instant.parse("2026-11-16T10:00:00Z"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.ZERO.setScale(2),
                List.of()
        );
    }
}
