package ru.pavelshadrin.shop.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_orders_event_product",
                columnNames = {"event_id", "product_id"}
        )
)
public class NotificationOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(name = "price", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "sale", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal sale;

    @Column(name = "total_price", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    protected NotificationOrder() {
    }

    public NotificationOrder(
            UUID eventId,
            UUID orderId,
            UUID productId,
            int quantity,
            BigDecimal price,
            BigDecimal sale,
            BigDecimal totalPrice,
            UUID userId
    ) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.sale = sale;
        this.totalPrice = totalPrice;
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getSale() {
        return sale;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public UUID getUserId() {
        return userId;
    }
}
