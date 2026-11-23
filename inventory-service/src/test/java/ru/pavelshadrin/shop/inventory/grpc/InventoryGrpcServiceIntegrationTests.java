package ru.pavelshadrin.shop.inventory.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.pavelshadrin.shop.contracts.inventory.v1.InventoryServiceGrpc;
import ru.pavelshadrin.shop.contracts.inventory.v1.ProductRequest;
import ru.pavelshadrin.shop.contracts.inventory.v1.ProductResponse;
import ru.pavelshadrin.shop.inventory.entity.Product;
import ru.pavelshadrin.shop.inventory.repository.ProductRepository;
import ru.pavelshadrin.shop.inventory.support.PostgreSqlIntegrationTest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class InventoryGrpcServiceIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private Server grpcServer;

    @Autowired
    private ProductRepository productRepository;

    private ManagedChannel channel;
    private InventoryServiceGrpc.InventoryServiceBlockingStub grpcClient;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        channel = ManagedChannelBuilder
                .forAddress("127.0.0.1", grpcServer.getPort())
                .usePlaintext()
                .build();
        grpcClient = InventoryServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void closeChannel() throws InterruptedException {
        if (channel != null) {
            channel.shutdownNow();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void sufficientQuantityReturnsExactProductSnapshotWithoutChangingStock() {
        Product product = saveProduct(
                "Ноутбук",
                10,
                "12345678901234567.89",
                "12.34"
        );

        ProductResponse response = grpcClient.checkAvailability(request(product.getId(), 7));

        assertThat(response.getProductId()).isEqualTo(product.getId().toString());
        assertThat(response.getName()).isEqualTo("Ноутбук");
        assertThat(response.getAvailableQuantity()).isEqualTo(10);
        assertThat(response.getPrice()).isEqualTo("12345678901234567.89");
        assertThat(response.getSale()).isEqualTo("12.34");
        assertThat(response.getAvailable()).isTrue();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getQuantity())
                .isEqualTo(10);
    }

    @Test
    void insufficientQuantityReturnsSuccessfulResponseWithAvailableFalse() {
        Product product = saveProduct("Монитор", 3, "49999.90", "5.00");

        ProductResponse response = grpcClient.checkAvailability(request(product.getId(), 4));

        assertThat(response.getAvailableQuantity()).isEqualTo(3);
        assertThat(response.getAvailable()).isFalse();
    }

    @Test
    void missingProductReturnsNotFound() {
        ProductRequest request = request(UUID.randomUUID(), 1);

        assertThatThrownBy(() -> grpcClient.checkAvailability(request))
                .isInstanceOfSatisfying(StatusRuntimeException.class, exception ->
                        assertThat(exception.getStatus().getCode())
                                .isEqualTo(Status.Code.NOT_FOUND));
    }

    @Test
    void invalidProductIdReturnsInvalidArgument() {
        ProductRequest request = ProductRequest.newBuilder()
                .setProductId("not-a-uuid")
                .setRequestedQuantity(1)
                .build();

        assertThatThrownBy(() -> grpcClient.checkAvailability(request))
                .isInstanceOfSatisfying(StatusRuntimeException.class, exception ->
                        assertThat(exception.getStatus().getCode())
                                .isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    @Test
    void nonPositiveQuantityReturnsInvalidArgument() {
        ProductRequest request = request(UUID.randomUUID(), 0);

        assertThatThrownBy(() -> grpcClient.checkAvailability(request))
                .isInstanceOfSatisfying(StatusRuntimeException.class, exception ->
                        assertThat(exception.getStatus().getCode())
                                .isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    private Product saveProduct(String name, int quantity, String price, String sale) {
        return productRepository.saveAndFlush(new Product(
                name,
                quantity,
                new BigDecimal(price),
                new BigDecimal(sale)
        ));
    }

    private ProductRequest request(UUID productId, int requestedQuantity) {
        return ProductRequest.newBuilder()
                .setProductId(productId.toString())
                .setRequestedQuantity(requestedQuantity)
                .build();
    }
}
