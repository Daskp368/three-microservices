package ru.pavelshadrin.shop.notification.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pavelshadrin.shop.notification.dto.order.NotificationOrderResponse;
import ru.pavelshadrin.shop.notification.service.NotificationOrderService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/orders", produces = MediaType.APPLICATION_JSON_VALUE)
public class NotificationOrderController {

    private final NotificationOrderService orderService;

    public NotificationOrderController(NotificationOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/all")
    public List<NotificationOrderResponse> findAllOrders() {
        return orderService.findAll();
    }

    @GetMapping("/by-order/{orderId}")
    public List<NotificationOrderResponse> findOrdersByOrderId(
            @PathVariable("orderId") UUID orderId
    ) {
        return orderService.findByOrderId(orderId);
    }

    @GetMapping("/by-user/{userId}")
    public List<NotificationOrderResponse> findOrdersByUserId(
            @PathVariable("userId") UUID userId
    ) {
        return orderService.findByUserId(userId);
    }
}
