package ru.pavelshadrin.shop.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfiguration {

    private static final long RETRY_INTERVAL_MILLIS = 1_000L;

    @Bean
    public CommonErrorHandler notificationKafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(
                RETRY_INTERVAL_MILLIS,
                FixedBackOff.UNLIMITED_ATTEMPTS
        ));
    }
}
