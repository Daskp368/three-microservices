package ru.pavelshadrin.shop.notification.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import ru.pavelshadrin.shop.notification.support.PostgreSqlIntegrationTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.pavelshadrin.shop.notification.entity.NotificationOrder;
import ru.pavelshadrin.shop.notification.repository.NotificationOrderRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationOrderControllerIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationOrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    @Test
    void allReturnsEveryRowUsingRestDto() throws Exception {
        UUID firstOrderId = UUID.randomUUID();
        UUID secondOrderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        NotificationOrder first = save(firstOrderId, UUID.randomUUID(), userId, "1200.00", "15.00", "2040.00");
        NotificationOrder second = save(secondOrderId, UUID.randomUUID(), userId, "499.90", "0.00", "1499.70");

        MvcResult result = mockMvc.perform(get("/api/orders/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode rows = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(rows).hasSize(2);
        assertThat(rows.findValuesAsText("id"))
                .containsExactlyInAnyOrder(first.getId().toString(), second.getId().toString());
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.has("eventId")).isFalse();
            assertThat(row.has("orderId")).isTrue();
            assertThat(row.has("productId")).isTrue();
            assertThat(row.has("quantity")).isTrue();
            assertThat(row.has("price")).isTrue();
            assertThat(row.has("sale")).isTrue();
            assertThat(row.has("totalPrice")).isTrue();
            assertThat(row.has("userId")).isTrue();
        });
    }

    @Test
    void byOrderReturnsOnlyRowsOfRequestedOrder() throws Exception {
        UUID requestedOrderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();

        save(requestedOrderId, firstProductId, userId, "100.00", "0.00", "100.00");
        save(requestedOrderId, secondProductId, userId, "200.00", "10.00", "180.00");
        save(UUID.randomUUID(), UUID.randomUUID(), userId, "300.00", "0.00", "300.00");

        MvcResult result = mockMvc.perform(get("/api/orders/by-order/{orderId}", requestedOrderId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rows = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(row ->
                assertThat(row.get("orderId").asText()).isEqualTo(requestedOrderId.toString())
        );
        assertThat(rows.findValuesAsText("productId"))
                .containsExactlyInAnyOrder(firstProductId.toString(), secondProductId.toString());
    }

    @Test
    void byUserReturnsOrdersAcrossRequestedUsersOrders() throws Exception {
        UUID requestedUserId = UUID.randomUUID();
        UUID firstOrderId = UUID.randomUUID();
        UUID secondOrderId = UUID.randomUUID();

        save(firstOrderId, UUID.randomUUID(), requestedUserId, "100.00", "0.00", "100.00");
        save(secondOrderId, UUID.randomUUID(), requestedUserId, "200.00", "0.00", "200.00");
        save(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "300.00", "0.00", "300.00");

        MvcResult result = mockMvc.perform(get("/api/orders/by-user/{userId}", requestedUserId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rows = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(row ->
                assertThat(row.get("userId").asText()).isEqualTo(requestedUserId.toString())
        );
        assertThat(rows.findValuesAsText("orderId"))
                .containsExactlyInAnyOrder(firstOrderId.toString(), secondOrderId.toString());
    }

    @Test
    void missingSelectionsReturnEmptyArrays() throws Exception {
        mockMvc.perform(get("/api/orders/all"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(get("/api/orders/by-order/{orderId}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(get("/api/orders/by-user/{userId}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void invalidIdsReturnContractError() throws Exception {
        mockMvc.perform(get("/api/orders/by-order/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_ID"))
                .andExpect(jsonPath("$.path").value("/api/orders/by-order/not-a-uuid"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());

        mockMvc.perform(get("/api/orders/by-user/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_ID"))
                .andExpect(jsonPath("$.path").value("/api/orders/by-user/not-a-uuid"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void writeMethodsAreNotExposed() throws Exception {
        mockMvc.perform(post("/api/orders/all"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/orders/all"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/orders/all"))
                .andExpect(status().isMethodNotAllowed());
    }

    private NotificationOrder save(
            UUID orderId,
            UUID productId,
            UUID userId,
            String price,
            String sale,
            String totalPrice
    ) {
        return orderRepository.saveAndFlush(new NotificationOrder(
                UUID.randomUUID(),
                orderId,
                productId,
                1,
                new BigDecimal(price),
                new BigDecimal(sale),
                new BigDecimal(totalPrice),
                userId
        ));
    }
}
