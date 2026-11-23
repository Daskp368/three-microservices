package ru.pavelshadrin.shop.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pavelshadrin.shop.notification.dto.order.NotificationOrderResponse;
import ru.pavelshadrin.shop.notification.entity.NotificationOrder;
import ru.pavelshadrin.shop.notification.event.OrderCreatedEvent;
import ru.pavelshadrin.shop.notification.repository.NotificationOrderRepository;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationOrderService {

    private final NotificationOrderRepository orderRepository;

    public NotificationOrderService(NotificationOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderEventSaveResult save(OrderCreatedEvent event) {
        if (orderRepository.existsByEventId(event.eventId())) {
            return OrderEventSaveResult.ALREADY_PROCESSED;
        }

        List<NotificationOrder> rows = event.items().stream()
                .map(item -> new NotificationOrder(
                        event.eventId(),
                        event.orderId(),
                        item.productId(),
                        item.quantity(),
                        item.price(),
                        item.sale(),
                        item.totalPrice(),
                        event.userId()
                ))
                .toList();

        orderRepository.saveAllAndFlush(rows);
        return OrderEventSaveResult.SAVED;
    }

    @Transactional(readOnly = true)
    public List<NotificationOrderResponse> findAll() {
        return toResponses(orderRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<NotificationOrderResponse> findByOrderId(UUID orderId) {
        return toResponses(orderRepository.findAllByOrderId(orderId));
    }

    @Transactional(readOnly = true)
    public List<NotificationOrderResponse> findByUserId(UUID userId) {
        return toResponses(orderRepository.findAllByUserId(userId));
    }

    private List<NotificationOrderResponse> toResponses(List<NotificationOrder> orders) {
        return orders.stream()
                .map(order -> new NotificationOrderResponse(
                        order.getId(),
                        order.getOrderId(),
                        order.getProductId(),
                        order.getQuantity(),
                        order.getPrice(),
                        order.getSale(),
                        order.getTotalPrice(),
                        order.getUserId()
                ))
                .toList();
    }
}
