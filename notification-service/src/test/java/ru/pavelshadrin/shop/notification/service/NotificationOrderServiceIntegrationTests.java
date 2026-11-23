package ru.pavelshadrin.shop.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import ru.pavelshadrin.shop.notification.support.PostgreSqlIntegrationTest;
import ru.pavelshadrin.shop.notification.entity.NotificationOrder;
import ru.pavelshadrin.shop.notification.event.OrderCreatedEvent;
import ru.pavelshadrin.shop.notification.event.OrderCreatedItemEvent;
import ru.pavelshadrin.shop.notification.repository.NotificationOrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class NotificationOrderServiceIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private NotificationOrderService orderService;

    @Autowired
    private NotificationOrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    @Test
    void saveEveryEventItemAsIndependentReadModelRow() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        OrderCreatedEvent event = twoItemEvent(
                eventId,
                orderId,
                userId,
                firstProductId,
                secondProductId
        );

        OrderEventSaveResult result = orderService.save(event);

        assertThat(result).isEqualTo(OrderEventSaveResult.SAVED);
        assertThat(orderRepository.count()).isEqualTo(2);
        assertThat(orderRepository.findAll()).allSatisfy(row -> {
            assertThat(row.getId()).isNotNull();
            assertThat(row.getEventId()).isEqualTo(eventId);
            assertThat(row.getOrderId()).isEqualTo(orderId);
            assertThat(row.getUserId()).isEqualTo(userId);
        });

        NotificationOrder firstRow = orderRepository.findAll().stream()
                .filter(row -> row.getProductId().equals(firstProductId))
                .findFirst()
                .orElseThrow();
        assertThat(firstRow.getQuantity()).isEqualTo(2);
        assertThat(firstRow.getPrice()).isEqualByComparingTo("1200.00");
        assertThat(firstRow.getSale()).isEqualByComparingTo("15.00");
        assertThat(firstRow.getTotalPrice()).isEqualByComparingTo("2040.00");
    }

    @Test
    void ignoreRepeatedDeliveryOfTheSameEvent() {
        OrderCreatedEvent event = twoItemEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        assertThat(orderService.save(event)).isEqualTo(OrderEventSaveResult.SAVED);
        assertThat(orderService.save(event)).isEqualTo(OrderEventSaveResult.ALREADY_PROCESSED);

        assertThat(orderRepository.count()).isEqualTo(2);
        assertThat(orderRepository.countByEventId(event.eventId())).isEqualTo(2);
    }

    @Test
    void rollbackAllRowsWhenOneItemViolatesDatabaseConstraint() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                1,
                Instant.parse("2026-11-16T10:00:00Z"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                List.of(
                        new OrderCreatedItemEvent(
                                UUID.randomUUID(),
                                1,
                                new BigDecimal("100.00"),
                                new BigDecimal("0.00"),
                                new BigDecimal("100.00")
                        ),
                        new OrderCreatedItemEvent(
                                UUID.randomUUID(),
                                -1,
                                new BigDecimal("0.00"),
                                new BigDecimal("0.00"),
                                new BigDecimal("0.00")
                        )
                )
        );

        assertThatThrownBy(() -> orderService.save(event))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(orderRepository.countByEventId(eventId)).isZero();
    }

    private OrderCreatedEvent twoItemEvent(
            UUID eventId,
            UUID orderId,
            UUID userId,
            UUID firstProductId,
            UUID secondProductId
    ) {
        return new OrderCreatedEvent(
                eventId,
                1,
                Instant.parse("2026-11-16T10:00:00Z"),
                orderId,
                userId,
                new BigDecimal("3539.70"),
                List.of(
                        new OrderCreatedItemEvent(
                                firstProductId,
                                2,
                                new BigDecimal("1200.00"),
                                new BigDecimal("15.00"),
                                new BigDecimal("2040.00")
                        ),
                        new OrderCreatedItemEvent(
                                secondProductId,
                                3,
                                new BigDecimal("499.90"),
                                new BigDecimal("0.00"),
                                new BigDecimal("1499.70")
                        )
                )
        );
    }
}
