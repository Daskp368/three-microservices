package ru.pavelshadrin.shop.order.dto.user;

import ru.pavelshadrin.shop.order.entity.UserRole;

import java.util.UUID;

public class UserResponse {

    private final UUID id;
    private final String username;
    private final String email;
    private final UserRole role;

    public UserResponse(UUID id, String username, String email, UserRole role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }
}
