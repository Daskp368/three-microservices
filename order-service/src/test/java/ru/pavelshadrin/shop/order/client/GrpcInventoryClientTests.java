package ru.pavelshadrin.shop.order.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.pavelshadrin.shop.contracts.inventory.v1.InventoryServiceGrpc;
import ru.pavelshadrin.shop.contracts.inventory.v1.ProductRequest;
import ru.pavelshadrin.shop.contracts.inventory.v1.ProductResponse;
import ru.pavelshadrin.shop.order.config.InventoryGrpcProperties;
import ru.pavelshadrin.shop.order.exception.InventoryRequestValidationException;
import ru.pavelshadrin.shop.order.exception.InventoryTimeoutException;
import ru.pavelshadrin.shop.order.exception.InventoryUnavailableException;
import ru.pavelshadrin.shop.order.exception.ProductNotFoundException;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrpcInventoryClientTests {

    private Server server;
    private ManagedChannel channel;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void returnValidatedProductSnapshot() throws Exception {
        UUID productId = UUID.randomUUID();
        GrpcInventoryClient client = client(serviceReturning(ProductResponse.newBuilder()
                .setProductId(productId.toString())
                .setName("Товар")
                .setAvailableQuantity(5)
                .setPrice("1200.00")
                .setSale("15.00")
                .setAvailable(true)
                .build()), Duration.ofSeconds(2));

        ProductSnapshot snapshot = client.checkAvailability(productId, 2);

        assertThat(snapshot.productId()).isEqualTo(productId);
        assertThat(snapshot.name()).isEqualTo("Товар");
        assertThat(snapshot.availableQuantity()).isEqualTo(5);
        assertThat(snapshot.price()).isEqualByComparingTo("1200.00");
        assertThat(snapshot.sale()).isEqualByComparingTo("15.00");
        assertThat(snapshot.available()).isTrue();
    }

    @Test
    void mapNotFoundStatus() throws Exception {
        UUID productId = UUID.randomUUID();
        GrpcInventoryClient client = client(serviceFailing(Status.NOT_FOUND), Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.checkAvailability(productId, 1))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void mapInvalidArgumentStatus() throws Exception {
        GrpcInventoryClient client = client(
                serviceFailing(Status.INVALID_ARGUMENT),
                Duration.ofSeconds(2)
        );

        assertThatThrownBy(() -> client.checkAvailability(UUID.randomUUID(), 1))
                .isInstanceOf(InventoryRequestValidationException.class);
    }

    @Test
    void mapInternalStatusWithoutLeakingServerDetails() throws Exception {
        GrpcInventoryClient client = client(
                serviceFailing(Status.INTERNAL.withDescription("database password")),
                Duration.ofSeconds(2)
        );

        assertThatThrownBy(() -> client.checkAvailability(UUID.randomUUID(), 1))
                .isInstanceOf(InventoryUnavailableException.class)
                .hasMessage("Inventory Service временно недоступен")
                .hasMessageNotContaining("database password");
    }

    @Test
    void mapUnavailableStatus() throws Exception {
        GrpcInventoryClient client = client(
                serviceFailing(Status.UNAVAILABLE.withDescription("connection refused")),
                Duration.ofSeconds(2)
        );

        assertThatThrownBy(() -> client.checkAvailability(UUID.randomUUID(), 1))
                .isInstanceOf(InventoryUnavailableException.class)
                .hasMessage("Inventory Service временно недоступен")
                .hasMessageNotContaining("connection refused");
    }

    @Test
    void mapExceededDeadline() throws Exception {
        GrpcInventoryClient client = client(new InventoryServiceGrpc.InventoryServiceImplBase() {
            @Override
            public void checkAvailability(
                    ProductRequest request,
                    StreamObserver<ProductResponse> responseObserver
            ) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                responseObserver.onNext(ProductResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }
        }, Duration.ofMillis(20));

        assertThatThrownBy(() -> client.checkAvailability(UUID.randomUUID(), 1))
                .isInstanceOf(InventoryTimeoutException.class);
    }

    @Test
    void rejectMalformedSuccessfulResponse() throws Exception {
        UUID productId = UUID.randomUUID();
        GrpcInventoryClient client = client(serviceReturning(ProductResponse.newBuilder()
                .setProductId(productId.toString())
                .setName("Товар")
                .setAvailableQuantity(1)
                .setPrice("not-a-number")
                .setSale("0.00")
                .setAvailable(true)
                .build()), Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.checkAvailability(productId, 1))
                .isInstanceOf(InventoryUnavailableException.class);
    }

    private GrpcInventoryClient client(
            InventoryServiceGrpc.InventoryServiceImplBase service,
            Duration deadline
    ) throws Exception {
        server = ServerBuilder.forPort(0)
                .addService(service)
                .build()
                .start();
        channel = ManagedChannelBuilder.forAddress("127.0.0.1", server.getPort())
                .usePlaintext()
                .build();

        InventoryGrpcProperties properties = new InventoryGrpcProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(server.getPort());
        properties.setDeadline(deadline);

        return new GrpcInventoryClient(
                InventoryServiceGrpc.newBlockingStub(channel),
                properties
        );
    }

    private InventoryServiceGrpc.InventoryServiceImplBase serviceReturning(
            ProductResponse response
    ) {
        return new InventoryServiceGrpc.InventoryServiceImplBase() {
            @Override
            public void checkAvailability(
                    ProductRequest request,
                    StreamObserver<ProductResponse> responseObserver
            ) {
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }

    private InventoryServiceGrpc.InventoryServiceImplBase serviceFailing(Status status) {
        return new InventoryServiceGrpc.InventoryServiceImplBase() {
            @Override
            public void checkAvailability(
                    ProductRequest request,
                    StreamObserver<ProductResponse> responseObserver
            ) {
                responseObserver.onError(status.asRuntimeException());
            }
        };
    }
}
