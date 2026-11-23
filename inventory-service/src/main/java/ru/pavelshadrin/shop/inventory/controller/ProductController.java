package ru.pavelshadrin.shop.inventory.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pavelshadrin.shop.inventory.dto.product.ProductRequest;
import ru.pavelshadrin.shop.inventory.dto.product.ProductResponse;
import ru.pavelshadrin.shop.inventory.service.ProductService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/products", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest productRequest) {
        ProductResponse response = productService.create(productRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ProductResponse> findAllProducts() {
        return productService.findAll();
    }

    @GetMapping("/{productId}")
    public ProductResponse findProductById(@PathVariable UUID productId) {
        return productService.findById(productId);
    }

    @PutMapping("/{productId}")
    public ProductResponse updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest productRequest) {
        return productService.update(productId, productRequest);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID productId) {
        productService.delete(productId);
        return ResponseEntity.noContent().build();
    }
}
