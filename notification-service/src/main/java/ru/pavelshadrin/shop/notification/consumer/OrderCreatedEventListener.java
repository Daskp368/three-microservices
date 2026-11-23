package ru.pavelshadrin.shop.notification.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.pavelshadrin.shop.notification.event.InvalidOrderCreatedEventException;
import ru.pavelshadrin.shop.notification.event.OrderCreatedEvent;
import ru.pavelshadrin.shop.notification.event.OrderCreatedEventParser;
import ru.pavelshadrin.shop.notification.service.NotificationOrderService;
import ru.pavelshadrin.shop.notification.service.OrderEventSaveResult;

@Component
public class OrderCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventListener.class);

    private final OrderCreatedEventParser eventParser;
    private final NotificationOrderService orderService;

    public OrderCreatedEventListener(
            OrderCreatedEventParser eventParser,
            NotificationOrderService orderService
    ) {
        this.eventParser = eventParser;
        this.orderService = orderService;
    }

    @KafkaListener(
            topics = "${app.kafka.orders-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            autoStartup = "${app.kafka.listener-enabled}"
    )
    public void onOrderCreated(ConsumerRecord<String, String> record) {
        OrderCreatedEvent event;
        try {
            event = eventParser.parse(record.key(), record.value());
        } catch (InvalidOrderCreatedEventException exception) {
            log.warn(
                    "Order event rejected: topic={}, partition={}, offset={}, reason={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    exception.getMessage()
            );
            return;
        }

        OrderEventSaveResult result = orderService.save(event);

        if (result == OrderEventSaveResult.ALREADY_PROCESSED) {
            log.info(
                    "Duplicate order event ignored: eventId={}, orderId={}, partition={}, offset={}",
                    event.eventId(),
                    event.orderId(),
                    record.partition(),
                    record.offset()
            );
            return;
        }

        log.info(
                "Order event saved: eventId={}, orderId={}, rows={}, partition={}, offset={}",
                event.eventId(),
                event.orderId(),
                event.items().size(),
                record.partition(),
                record.offset()
        );
    }
}
