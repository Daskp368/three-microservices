package ru.pavelshadrin.shop.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.pavelshadrin.shop.order.support.PostgreSqlIntegrationTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.pavelshadrin.shop.order.client.ProductSnapshot;
import ru.pavelshadrin.shop.order.entity.Order;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.entity.UserRole;
import ru.pavelshadrin.shop.order.outbox.OrderOutboxMessageFactory;
import ru.pavelshadrin.shop.order.outbox.OutboxMessageRepository;
import ru.pavelshadrin.shop.order.repository.OrderRepository;
import ru.pavelshadrin.shop.order.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class OrderOutboxAtomicityIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private OrderPersistenceService persistenceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @MockitoBean
    private OrderOutboxMessageFactory outboxMessageFactory;

    @BeforeEach
    void cleanDatabase() {
        outboxMessageRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void rollbackOrderWhenOutboxCreationFails() {
        User user = userRepository.saveAndFlush(new User(
                "rollback-user",
                "hash",
                "rollback-user@example.com",
                UserRole.USER
        ));
        ProductSnapshot product = new ProductSnapshot(
                UUID.randomUUID(),
                "Товар",
                10,
                new BigDecimal("100.00"),
                new BigDecimal("0.00"),
                true
        );
        CalculatedOrderItem item = new CalculatedOrderItem(
                product,
                1,
                new BigDecimal("100.00"),
                new BigDecimal("100.00")
        );
        when(outboxMessageFactory.create(any(Order.class)))
                .thenThrow(new IllegalStateException("serialization failed"));

        assertThatThrownBy(() -> persistenceService.save(
                user.getId(),
                List.of(item),
                new BigDecimal("100.00")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("serialization failed");

        assertThat(orderRepository.count()).isZero();
        assertThat(outboxMessageRepository.count()).isZero();
        assertThat(userRepository.count()).isEqualTo(1);
    }
}
