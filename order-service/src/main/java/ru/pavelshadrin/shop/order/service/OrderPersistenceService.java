package ru.pavelshadrin.shop.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pavelshadrin.shop.order.dto.order.OrderResponse;
import ru.pavelshadrin.shop.order.entity.Order;
import ru.pavelshadrin.shop.order.entity.OrderItem;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.exception.UserNotFoundException;
import ru.pavelshadrin.shop.order.mapper.OrderMapper;
import ru.pavelshadrin.shop.order.repository.OrderRepository;
import ru.pavelshadrin.shop.order.repository.UserRepository;
import ru.pavelshadrin.shop.order.outbox.OrderOutboxMessageFactory;
import ru.pavelshadrin.shop.order.outbox.OutboxMessage;
import ru.pavelshadrin.shop.order.outbox.OutboxMessageRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderPersistenceService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OutboxMessageRepository outboxMessageRepository;
    private final OrderOutboxMessageFactory outboxMessageFactory;

    public OrderPersistenceService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            OutboxMessageRepository outboxMessageRepository,
            OrderOutboxMessageFactory outboxMessageFactory
    ) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.outboxMessageRepository = outboxMessageRepository;
        this.outboxMessageFactory = outboxMessageFactory;
    }

    @Transactional
    public OrderResponse save(
            UUID userId,
            List<CalculatedOrderItem> calculatedItems,
            BigDecimal totalPrice
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Order order = new Order(user, totalPrice);

        for (CalculatedOrderItem calculatedItem : calculatedItems) {
            order.addItem(toEntity(calculatedItem));
        }

        Order savedOrder = orderRepository.saveAndFlush(order);
        OutboxMessage outboxMessage = outboxMessageFactory.create(savedOrder);
        outboxMessageRepository.saveAndFlush(outboxMessage);
        return orderMapper.toResponse(savedOrder);
    }

    private OrderItem toEntity(CalculatedOrderItem item) {
        return new OrderItem(
                item.product().productId(),
                item.product().name(),
                item.quantity(),
                item.product().price(),
                item.product().sale(),
                item.discountedUnitPrice(),
                item.totalPrice()
        );
    }
}
