package ru.pavelshadrin.shop.order.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import ru.pavelshadrin.shop.order.support.PostgreSqlIntegrationTest;
import ru.pavelshadrin.shop.order.entity.Order;
import ru.pavelshadrin.shop.order.entity.OrderItem;
import ru.pavelshadrin.shop.order.entity.OrderStatus;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.entity.UserRole;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderPersistenceIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveOrderWithMultipleItemsByCascade() {
        User user = new User(
                "order-test-user",
                "bcrypt-hash-placeholder",
                "order-test@example.com",
                UserRole.USER
        );

        User savedUser = userRepository.saveAndFlush(user);

        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();

        Order order = new Order(savedUser, new BigDecimal("3539.70"));

        OrderItem firstItem = new OrderItem(
                firstProductId,
                "Товар A",
                2,
                new BigDecimal("1200.00"),
                new BigDecimal("15.00"),
                new BigDecimal("1020.00"),
                new BigDecimal("2040.00")
        );

        OrderItem secondItem = new OrderItem(
                secondProductId,
                "Товар B",
                3,
                new BigDecimal("499.90"),
                new BigDecimal("0.00"),
                new BigDecimal("499.90"),
                new BigDecimal("1499.70")
        );

        order.addItem(firstItem);
        order.addItem(secondItem);

        Order savedOrder = orderRepository.saveAndFlush(order);

        UUID userId = savedUser.getId();
        UUID orderId = savedOrder.getId();

        assertThat(orderId).isNotNull();
        assertThat(firstItem.getId()).isNotNull();
        assertThat(secondItem.getId()).isNotNull();

        entityManager.clear();

        Order loadedOrder = orderRepository.findById(orderId).orElseThrow();

        assertThat(loadedOrder.getUser().getId()).isEqualTo(userId);
        assertThat(loadedOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(loadedOrder.getTotalPrice()).isEqualByComparingTo("3539.70");
        assertThat(loadedOrder.getCreatedAt()).isNotNull();
        assertThat(loadedOrder.getItems()).hasSize(2);
        assertThat(loadedOrder.getItems())
                .extracting(OrderItem::getProductId)
                .containsExactlyInAnyOrder(firstProductId, secondProductId);

        OrderItem loadedFirstItem = loadedOrder.getItems().stream()
                .filter(item -> item.getProductId().equals(firstProductId))
                .findFirst()
                .orElseThrow();

        assertThat(loadedFirstItem.getOrder().getId()).isEqualTo(orderId);
        assertThat(loadedFirstItem.getProductName()).isEqualTo("Товар A");
        assertThat(loadedFirstItem.getQuantity()).isEqualTo(2);
        assertThat(loadedFirstItem.getPrice()).isEqualByComparingTo("1200.00");
        assertThat(loadedFirstItem.getSale()).isEqualByComparingTo("15.00");
        assertThat(loadedFirstItem.getDiscountedUnitPrice()).isEqualByComparingTo("1020.00");
        assertThat(loadedFirstItem.getTotalPrice()).isEqualByComparingTo("2040.00");
    }

    @Test
    void rejectDuplicateProductInsideOneOrder() {
        User user = userRepository.saveAndFlush(new User(
                "duplicate-product-user",
                "bcrypt-hash-placeholder",
                "duplicate-product@example.com",
                UserRole.USER
        ));

        UUID productId = UUID.randomUUID();
        Order order = new Order(user, new BigDecimal("20.00"));

        order.addItem(new OrderItem(
                productId,
                "Один товар",
                1,
                new BigDecimal("10.00"),
                new BigDecimal("0.00"),
                new BigDecimal("10.00"),
                new BigDecimal("10.00")
        ));
        order.addItem(new OrderItem(
                productId,
                "Один товар",
                1,
                new BigDecimal("10.00"),
                new BigDecimal("0.00"),
                new BigDecimal("10.00"),
                new BigDecimal("10.00")
        ));

        assertThatThrownBy(() -> orderRepository.saveAndFlush(order))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectDeletingUserThatHasOrder() {
        User user = userRepository.saveAndFlush(new User(
                "protected-user",
                "bcrypt-hash-placeholder",
                "protected-user@example.com",
                UserRole.USER
        ));

        Order order = new Order(user, new BigDecimal("500.00"));
        order.addItem(new OrderItem(
                UUID.randomUUID(),
                "Товар в заказе",
                1,
                new BigDecimal("500.00"),
                new BigDecimal("0.00"),
                new BigDecimal("500.00"),
                new BigDecimal("500.00")
        ));
        orderRepository.saveAndFlush(order);

        UUID userId = user.getId();
        entityManager.clear();

        assertThatThrownBy(() -> {
            userRepository.deleteById(userId);
            userRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
