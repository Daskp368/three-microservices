package ru.pavelshadrin.shop.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.pavelshadrin.shop.inventory.entity.Product;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
}
