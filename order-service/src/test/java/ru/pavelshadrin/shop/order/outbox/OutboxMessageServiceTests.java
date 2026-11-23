package ru.pavelshadrin.shop.order.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxMessageServiceTests {

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @InjectMocks
    private OutboxMessageService service;

    @Test
    void findPendingMessagesInCreationOrder() {
        OutboxMessage message = mock(OutboxMessage.class);
        when(outboxMessageRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(message));

        assertThat(service.findPending()).containsExactly(message);
    }

    @Test
    void markPendingMessageAsPublished() {
        UUID messageId = UUID.randomUUID();
        Instant publishedAt = Instant.now();
        OutboxMessage message = mock(OutboxMessage.class);
        when(message.getStatus()).thenReturn(OutboxStatus.PENDING);
        when(outboxMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

        service.markPublished(messageId, publishedAt);

        verify(message).markPublished(publishedAt);
    }

    @Test
    void retainPendingMessageAndStorePublicationError() {
        UUID messageId = UUID.randomUUID();
        OutboxMessage message = mock(OutboxMessage.class);
        when(message.getStatus()).thenReturn(OutboxStatus.PENDING);
        when(outboxMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

        service.markFailed(messageId, "broker unavailable");

        verify(message).markFailed("broker unavailable");
    }
}
