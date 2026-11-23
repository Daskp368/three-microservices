package ru.pavelshadrin.shop.order.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.pavelshadrin.shop.order.support.PostgreSqlIntegrationTest;
import ru.pavelshadrin.shop.order.entity.Order;
import ru.pavelshadrin.shop.order.entity.OrderItem;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.entity.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findUserAndCheckUniqueFields() {
        User savedUser = userRepository.saveAndFlush(new User(
                "repository-user",
                "bcrypt-hash-placeholder",
                "repository-user@example.com",
                UserRole.USER
        ));

        assertThat(userRepository.findByUsername("repository-user"))
                .isPresent()
                .get()
                .extracting(User::getId)
                .isEqualTo(savedUser.getId());

        assertThat(userRepository.findByUsername("missing-user")).isEmpty();

        assertThat(userRepository.existsByUsername("repository-user")).isTrue();
        assertThat(userRepository.existsByUsername("missing-user")).isFalse();

        assertThat(userRepository.existsByEmail("repository-user@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();

        assertThat(userRepository.existsByUsernameAndIdNot(
                "repository-user",
                savedUser.getId()
        )).isFalse();

        assertThat(userRepository.existsByUsernameAndIdNot(
                "repository-user",
                UUID.randomUUID()
        )).isTrue();

        assertThat(userRepository.existsByEmailAndIdNot(
                "repository-user@example.com",
                savedUser.getId()
        )).isFalse();

        assertThat(userRepository.existsByEmailAndIdNot(
                "repository-user@example.com",
                UUID.randomUUID()
        )).isTrue();
    }

    @Test
    void updateProfileWithoutChangingPasswordAndCreationTime() {
        User savedUser = userRepository.saveAndFlush(new User(
                "profile-user",
                "unchanged-password-hash",
                "profile-user@example.com",
                UserRole.USER
        ));

        UUID userId = savedUser.getId();
        String originalPassword = savedUser.getPassword();
        Instant originalCreatedAt = savedUser.getCreatedAt();

        savedUser.updateProfile(
                "updated-user",
                "updated-user@example.com",
                UserRole.ADMIN
        );

        userRepository.flush();
        entityManager.clear();

        User updatedUser = userRepository.findById(userId).orElseThrow();

        assertThat(updatedUser.getUsername()).isEqualTo("updated-user");
        assertThat(updatedUser.getEmail()).isEqualTo("updated-user@example.com");
        assertThat(updatedUser.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(updatedUser.getPassword()).isEqualTo(originalPassword);
        assertThat(updatedUser.getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    void detectOrdersBelongingToUser() {
        User savedUser = userRepository.saveAndFlush(new User(
                "user-with-order",
                "bcrypt-hash-placeholder",
                "user-with-order@example.com",
                UserRole.USER
        ));

        assertThat(orderRepository.existsByUser_Id(savedUser.getId())).isFalse();

        Order order = new Order(savedUser, new BigDecimal("250.00"));
        order.addItem(new OrderItem(
                UUID.randomUUID(),
                "Тестовый товар",
                1,
                new BigDecimal("250.00"),
                new BigDecimal("0.00"),
                new BigDecimal("250.00"),
                new BigDecimal("250.00")
        ));

        orderRepository.saveAndFlush(order);
        entityManager.clear();

        assertThat(orderRepository.existsByUser_Id(savedUser.getId())).isTrue();
    }
}
