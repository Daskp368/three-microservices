package ru.pavelshadrin.shop.order.mapper;

import org.springframework.stereotype.Component;
import ru.pavelshadrin.shop.order.dto.user.UserResponse;
import ru.pavelshadrin.shop.order.entity.User;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}
