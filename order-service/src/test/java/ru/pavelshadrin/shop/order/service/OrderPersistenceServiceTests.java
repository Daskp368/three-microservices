package ru.pavelshadrin.shop.order.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.pavelshadrin.shop.order.client.ProductSnapshot;
import ru.pavelshadrin.shop.order.dto.order.OrderResponse;
import ru.pavelshadrin.shop.order.entity.Order;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.entity.UserRole;
import ru.pavelshadrin.shop.order.mapper.OrderMapper;
import ru.pavelshadrin.shop.order.outbox.OrderOutboxMessageFactory;
import ru.pavelshadrin.shop.order.outbox.OutboxMessage;
import ru.pavelshadrin.shop.order.outbox.OutboxMessageRepository;
import ru.pavelshadrin.shop.order.repository.OrderRepository;
import ru.pavelshadrin.shop.order.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @Mock
    private OrderOutboxMessageFactory outboxMessageFactory;

    @InjectMocks
    private OrderPersistenceService service;

    @Test
    void saveOrderAndSnapshotFields() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        User user = new User("user", "hash", "user@example.com", UserRole.USER);
        ProductSnapshot product = new ProductSnapshot(
                productId,
                "Товар",
                10,
                new BigDecimal("1200.00"),
                new BigDecimal("15.00"),
                true
        );
        CalculatedOrderItem item = new CalculatedOrderItem(
                product,
                2,
                new BigDecimal("1020.00"),
                new BigDecimal("2040.00")
        );
        OrderResponse response = org.mockito.Mockito.mock(OrderResponse.class);
        OutboxMessage outboxMessage = org.mockito.Mockito.mock(OutboxMessage.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(orderRepository.saveAndFlush(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxMessageFactory.create(any(Order.class))).thenReturn(outboxMessage);
        when(outboxMessageRepository.saveAndFlush(outboxMessage)).thenReturn(outboxMessage);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(response);

        OrderResponse actual = service.save(
                userId,
                List.of(item),
                new BigDecimal("2040.00")
        );

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveAndFlush(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(actual).isSameAs(response);
        assertThat(savedOrder.getUser()).isSameAs(user);
        assertThat(savedOrder.getTotalPrice()).isEqualByComparingTo("2040.00");
        assertThat(savedOrder.getItems()).singleElement().satisfies(savedItem -> {
            assertThat(savedItem.getProductId()).isEqualTo(productId);
            assertThat(savedItem.getProductName()).isEqualTo("Товар");
            assertThat(savedItem.getQuantity()).isEqualTo(2);
            assertThat(savedItem.getPrice()).isEqualByComparingTo("1200.00");
            assertThat(savedItem.getSale()).isEqualByComparingTo("15.00");
            assertThat(savedItem.getDiscountedUnitPrice()).isEqualByComparingTo("1020.00");
            assertThat(savedItem.getTotalPrice()).isEqualByComparingTo("2040.00");
            assertThat(savedItem.getOrder()).isSameAs(savedOrder);
        });
        verify(outboxMessageFactory).create(savedOrder);
        verify(outboxMessageRepository).saveAndFlush(outboxMessage);
        verify(orderMapper).toResponse(savedOrder);
    }
}
