package ru.pavelshadrin.shop.inventory.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pavelshadrin.shop.contracts.inventory.v1.InventoryServiceGrpc;
import ru.pavelshadrin.shop.contracts.inventory.v1.ProductRequest;
import ru.pavelshadrin.shop.contracts.inventory.v1.ProductResponse;
import ru.pavelshadrin.shop.inventory.entity.Product;
import ru.pavelshadrin.shop.inventory.repository.ProductRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final ProductRepository productRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryGrpcService.class);

    public InventoryGrpcService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void checkAvailability(ProductRequest request,
            StreamObserver<ProductResponse> responseObserver) {
        String rawProductId = request.getProductId();
        UUID productId;

        try {
            productId = UUID.fromString(rawProductId);
            if (!productId.toString().equalsIgnoreCase(rawProductId)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("product_id must be a valid UUID")
                            .asRuntimeException());
            return;
        }

        if (request.getRequestedQuantity() <= 0) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("requested_quantity must be greater than 0")
                            .asRuntimeException()
            );
            return;
        }

        Optional<Product> productOptional;

        try {
            productOptional = productRepository.findById(productId);
        } catch (RuntimeException exception) {
            handleUnexpectedError(exception, responseObserver);
            return;
        }

        if (productOptional.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("product not found")
                            .asRuntimeException()
            );
            return;
        }

        Product product = productOptional.get();
        ProductResponse response;

        try {
            boolean available = product.getQuantity() >= request.getRequestedQuantity();

            response = ProductResponse.newBuilder()
                    .setProductId(product.getId().toString())
                    .setName(product.getName())
                    .setAvailableQuantity(product.getQuantity())
                    .setPrice(product.getPrice().toPlainString())
                    .setSale(product.getSale().toPlainString())
                    .setAvailable(available)
                    .build();
        } catch (RuntimeException exception) {
            handleUnexpectedError(exception, responseObserver);
            return;
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void handleUnexpectedError(
            RuntimeException exception,
            StreamObserver<ProductResponse> responseObserver
    ) {
        LOGGER.error("Unexpected error while checking product availability", exception);
        responseObserver.onError(
                Status.INTERNAL
                        .withDescription("internal server error")
                        .asRuntimeException()
        );
    }
}
