package ru.pavelshadrin.shop.order.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.pavelshadrin.shop.contracts.inventory.v1.InventoryServiceGrpc;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(InventoryGrpcProperties.class)
public class InventoryGrpcClientConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ManagedChannel inventoryManagedChannel(
            InventoryGrpcProperties properties
    ) {
        return ManagedChannelBuilder
                .forAddress(properties.getHost(), properties.getPort())
                .usePlaintext()
                .build();
    }

    @Bean
    public InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceBlockingStub(
            ManagedChannel inventoryManagedChannel
    ) {
        return InventoryServiceGrpc.newBlockingStub(inventoryManagedChannel);
    }
}
