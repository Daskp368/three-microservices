package ru.pavelshadrin.shop.order.config;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import ru.pavelshadrin.shop.contracts.inventory.v1.InventoryServiceGrpc;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryGrpcClientConfigurationTests {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(InventoryGrpcClientConfiguration.class)
                    .withPropertyValues(
                            "app.inventory.grpc.host=127.0.0.1",
                            "app.inventory.grpc.port=19090",
                            "app.inventory.grpc.deadline=1500ms"
                    );

    @Test
    void createChannelStubAndBindProperties() {
        AtomicReference<ManagedChannel> channelReference = new AtomicReference<>();

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(InventoryGrpcProperties.class);
            assertThat(context).hasSingleBean(ManagedChannel.class);
            assertThat(context).hasSingleBean(
                    InventoryServiceGrpc.InventoryServiceBlockingStub.class
            );

            InventoryGrpcProperties properties =
                    context.getBean(InventoryGrpcProperties.class);
            ManagedChannel channel = context.getBean(ManagedChannel.class);
            InventoryServiceGrpc.InventoryServiceBlockingStub stub =
                    context.getBean(
                            InventoryServiceGrpc.InventoryServiceBlockingStub.class
                    );

            channelReference.set(channel);

            assertThat(properties.getHost()).isEqualTo("127.0.0.1");
            assertThat(properties.getPort()).isEqualTo(19090);
            assertThat(properties.getDeadline()).isEqualTo(Duration.ofMillis(1500));
            assertThat(stub.getChannel()).isSameAs(channel);
            assertThat(channel.isShutdown()).isFalse();
        });

        assertThat(channelReference.get()).isNotNull();
        assertThat(channelReference.get().isShutdown()).isTrue();
    }

    @Test
    void rejectInvalidClientConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(InventoryGrpcClientConfiguration.class)
                .withPropertyValues(
                        "app.inventory.grpc.host= ",
                        "app.inventory.grpc.port=0",
                        "app.inventory.grpc.deadline=0ms"
                )
                .run(context -> assertThat(context).hasFailed());
    }
}
