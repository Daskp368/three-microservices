package ru.pavelshadrin.shop.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.pavelshadrin.shop.order.dto.user.UpdateUserRequest;
import ru.pavelshadrin.shop.order.dto.user.UserResponse;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.entity.UserRole;
import ru.pavelshadrin.shop.order.exception.UserHasOrdersException;
import ru.pavelshadrin.shop.order.exception.UserNotFoundException;
import ru.pavelshadrin.shop.order.mapper.UserMapper;
import ru.pavelshadrin.shop.order.repository.OrderRepository;
import ru.pavelshadrin.shop.order.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, orderRepository, new UserMapper());
    }

    @Test
    void updateOnlyContractFields() {
        UUID userId = UUID.randomUUID();
        User user = new User("old-name", "password-hash", "old@example.com", UserRole.USER);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("new-name");
        request.setEmail("new@example.com");
        request.setRole(UserRole.ADMIN);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.update(userId, request);

        assertThat(response.getUsername()).isEqualTo("new-name");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getPassword()).isEqualTo("password-hash");
        verify(userRepository).flush();
    }

    @Test
    void rejectDeletingUserWithOrders() {
        UUID userId = UUID.randomUUID();
        User user = new User("ordered-user", "password-hash", "ordered@example.com", UserRole.USER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(orderRepository.existsByUser_Id(userId)).thenReturn(true);

        assertThatThrownBy(() -> userService.delete(userId))
                .isInstanceOf(UserHasOrdersException.class);
        verify(userRepository, never()).delete(user);
    }

    @Test
    void reportMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(UserNotFoundException.class);
    }
}
