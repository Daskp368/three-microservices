package ru.pavelshadrin.shop.order.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxMessageService {

    private final OutboxMessageRepository outboxMessageRepository;

    public OutboxMessageService(OutboxMessageRepository outboxMessageRepository) {
        this.outboxMessageRepository = outboxMessageRepository;
    }

    @Transactional(readOnly = true)
    public List<OutboxMessage> findPending() {
        return outboxMessageRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    }

    @Transactional
    public void markPublished(UUID messageId, Instant publishedAt) {
        OutboxMessage message = findById(messageId);
        if (message.getStatus() == OutboxStatus.PUBLISHED) {
            return;
        }
        message.markPublished(publishedAt);
    }

    @Transactional
    public void markFailed(UUID messageId, String error) {
        OutboxMessage message = findById(messageId);
        if (message.getStatus() == OutboxStatus.PUBLISHED) {
            return;
        }
        message.markFailed(normalizeError(error));
    }

    private OutboxMessage findById(UUID messageId) {
        return outboxMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalStateException(
                        "Outbox message not found: " + messageId
                ));
    }

    private String normalizeError(String error) {
        if (error == null || error.isBlank()) {
            return "Unknown Kafka publication error";
        }
        return error;
    }
}
