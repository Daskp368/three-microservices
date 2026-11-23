package ru.pavelshadrin.shop.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.pavelshadrin.shop.notification.entity.NotificationOrder;

import java.util.List;
import java.util.UUID;

public interface NotificationOrderRepository extends JpaRepository<NotificationOrder, UUID> {

    boolean existsByEventId(UUID eventId);

    long countByEventId(UUID eventId);

    List<NotificationOrder> findAllByOrderId(UUID orderId);

    List<NotificationOrder> findAllByUserId(UUID userId);
}
