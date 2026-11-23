package ru.pavelshadrin.shop.order.mapper;

import org.springframework.stereotype.Component;
import ru.pavelshadrin.shop.order.dto.order.OrderItemResponse;
import ru.pavelshadrin.shop.order.dto.order.OrderResponse;
import ru.pavelshadrin.shop.order.entity.Order;
import ru.pavelshadrin.shop.order.entity.OrderItem;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .map(this::toItemResponse)
                        .toList(),
                order.getTotalPrice()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice(),
                item.getSale(),
                item.getDiscountedUnitPrice(),
                item.getTotalPrice()
        );
    }
}
