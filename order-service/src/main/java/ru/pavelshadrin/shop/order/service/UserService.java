package ru.pavelshadrin.shop.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pavelshadrin.shop.order.dto.user.UpdateUserRequest;
import ru.pavelshadrin.shop.order.dto.user.UserResponse;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.exception.EmailAlreadyExistsException;
import ru.pavelshadrin.shop.order.exception.UserHasOrdersException;
import ru.pavelshadrin.shop.order.exception.UserNotFoundException;
import ru.pavelshadrin.shop.order.exception.UsernameAlreadyExistsException;
import ru.pavelshadrin.shop.order.mapper.UserMapper;
import ru.pavelshadrin.shop.order.repository.OrderRepository;
import ru.pavelshadrin.shop.order.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       OrderRepository orderRepository,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID userId) {
        return userMapper.toResponse(requireUser(userId));
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateUserRequest request) {
        User user = requireUser(userId);
        if (userRepository.existsByUsernameAndIdNot(request.getUsername(), userId)) {
            throw new UsernameAlreadyExistsException();
        }
        if (userRepository.existsByEmailAndIdNot(request.getEmail(), userId)) {
            throw new EmailAlreadyExistsException();
        }

        user.updateProfile(request.getUsername(), request.getEmail(), request.getRole());
        userRepository.flush();
        return userMapper.toResponse(user);
    }

    @Transactional
    public void delete(UUID userId) {
        User user = requireUser(userId);
        if (orderRepository.existsByUser_Id(userId)) {
            throw new UserHasOrdersException();
        }
        userRepository.delete(user);
        userRepository.flush();
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
