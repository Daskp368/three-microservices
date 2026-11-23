package ru.pavelshadrin.shop.inventory.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.pavelshadrin.shop.contracts.inventory.v1.ProductRequest;
import ru.pavelshadrin.shop.contracts.inventory.v1.ProductResponse;
import ru.pavelshadrin.shop.inventory.repository.ProductRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryGrpcServiceTests {

    @Test
    void repositoryFailureReturnsInternalWithoutImplementationDetails() {
        ProductRepository productRepository = mock(ProductRepository.class);
        StreamObserver<ProductResponse> responseObserver = mock();
        InventoryGrpcService service = new InventoryGrpcService(productRepository);
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId))
                .thenThrow(new IllegalStateException("SQL failure with internal details"));

        service.checkAvailability(
                ProductRequest.newBuilder()
                        .setProductId(productId.toString())
                        .setRequestedQuantity(1)
                        .build(),
                responseObserver
        );

        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(errorCaptor.capture());
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();

        Status status = Status.fromThrowable(errorCaptor.getValue());
        assertThat(status.getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(status.getDescription()).isEqualTo("internal server error");
        assertThat(status.getDescription()).doesNotContain("SQL");
    }
}
