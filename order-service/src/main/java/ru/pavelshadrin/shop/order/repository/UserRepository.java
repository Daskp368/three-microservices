package ru.pavelshadrin.shop.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.pavelshadrin.shop.order.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository  extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
