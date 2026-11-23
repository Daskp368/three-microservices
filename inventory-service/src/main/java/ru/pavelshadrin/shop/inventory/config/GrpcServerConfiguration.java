package ru.pavelshadrin.shop.inventory.config;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcServerConfiguration {

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server grpcServer(
            @Value("${inventory.grpc.port}") int grpcPort,
            ObjectProvider<BindableService> grpcServices
    ) {
        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(grpcPort);
        grpcServices.orderedStream().forEach(serverBuilder::addService);
        return serverBuilder.build();
    }
}
