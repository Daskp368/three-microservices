package ru.pavelshadrin.shop.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.pavelshadrin.shop.order.entity.Order;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByUser_Id(UUID userId);
}
