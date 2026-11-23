package ru.pavelshadrin.shop.inventory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pavelshadrin.shop.inventory.dto.product.ProductRequest;
import ru.pavelshadrin.shop.inventory.dto.product.ProductResponse;
import ru.pavelshadrin.shop.inventory.entity.Product;
import ru.pavelshadrin.shop.inventory.exception.ProductNotFoundException;
import ru.pavelshadrin.shop.inventory.repository.ProductRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(
                request.getName(),
                request.getQuantity(),
                request.getPrice(),
                request.getSale());
        Product savedProduct = productRepository.save(product);
        return toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID productId) {
        return toResponse(findProductById(productId));
    }

    @Transactional
    public ProductResponse update(UUID productId, ProductRequest request) {
        Product product = findProductById(productId);
        product.update(
                request.getName(),
                request.getQuantity(),
                request.getPrice(),
                request.getSale());
        return toResponse(product);
    }

    @Transactional
    public void delete(UUID productId) {
        Product product = findProductById(productId);
        productRepository.delete(product);
    }

    private Product findProductById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getQuantity(),
                product.getPrice(),
                product.getSale());
    }
}
