package ru.pavelshadrin.shop.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import ru.pavelshadrin.shop.order.support.PostgreSqlIntegrationTest;
import ru.pavelshadrin.shop.order.client.InventoryClient;
import ru.pavelshadrin.shop.order.client.ProductSnapshot;
import ru.pavelshadrin.shop.order.entity.User;
import ru.pavelshadrin.shop.order.entity.UserRole;
import ru.pavelshadrin.shop.order.exception.ProductNotFoundException;
import ru.pavelshadrin.shop.order.exception.InventoryTimeoutException;
import ru.pavelshadrin.shop.order.exception.InventoryUnavailableException;
import ru.pavelshadrin.shop.order.outbox.OutboxMessage;
import ru.pavelshadrin.shop.order.outbox.OutboxMessageRepository;
import ru.pavelshadrin.shop.order.repository.OrderRepository;
import ru.pavelshadrin.shop.order.repository.UserRepository;
import ru.pavelshadrin.shop.order.security.JwtService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Stage6OrderIntegrationTests extends PostgreSqlIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private InventoryClient inventoryClient;

    @BeforeEach
    void cleanDatabaseAndMock() {
        reset(inventoryClient);
        outboxMessageRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createCalculatedOrderForAuthenticatedUser() throws Exception {
        String accessToken = createUserAndAccessToken("order-user");
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        when(inventoryClient.checkAvailability(firstProductId, 2))
                .thenReturn(product(firstProductId, "Товар A", 10, "1200.00", "15.00", true));
        when(inventoryClient.checkAvailability(secondProductId, 3))
                .thenReturn(product(secondProductId, "Товар B", 3, "499.90", "0.00", true));

        ResponseEntity<String> response = createOrder(
                accessToken,
                orderBody(
                        item(firstProductId, 2),
                        item(secondProductId, 3)
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("orderId").asText()).isNotBlank();
        assertThat(json.get("status").asText()).isEqualTo("CREATED");
        assertThat(json.get("items")).hasSize(2);
        assertThat(json.get("items").get(0).get("discountedUnitPrice").decimalValue())
                .isEqualByComparingTo("1020.00");
        assertThat(json.get("totalPrice").decimalValue()).isEqualByComparingTo("3539.70");
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderRepository.findAll().get(0).getTotalPrice())
                .isEqualByComparingTo("3539.70");
        assertThat(outboxMessageRepository.count()).isEqualTo(1);

        OutboxMessage outboxMessage = outboxMessageRepository.findAll().get(0);
        assertThat(outboxMessage.getAggregateId())
                .isEqualTo(UUID.fromString(json.get("orderId").asText()));
        assertThat(outboxMessage.getPayload().get("eventVersion").asInt()).isEqualTo(1);
        assertThat(outboxMessage.getPayload().get("items")).hasSize(2);
        assertThat(outboxMessage.getPayload().get("totalPrice").decimalValue())
                .isEqualByComparingTo("3539.70");
    }

    @Test
    void requireAuthenticationBeforeCreatingOrder() {
        UUID productId = UUID.randomUUID();

        ResponseEntity<String> response = createOrder(
                null,
                orderBody(item(productId, 1))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("UNAUTHORIZED");
        verify(inventoryClient, never()).checkAvailability(productId, 1);
        assertThat(orderRepository.count()).isZero();
        assertThat(outboxMessageRepository.count()).isZero();
    }

    @Test
    void rejectInsufficientStockWithoutSavingOrder() {
        String accessToken = createUserAndAccessToken("stock-user");
        UUID productId = UUID.randomUUID();
        when(inventoryClient.checkAvailability(productId, 2))
                .thenReturn(product(productId, "Товар", 1, "100.00", "0.00", false));

        ResponseEntity<String> response = createOrder(
                accessToken,
                orderBody(item(productId, 2))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("INSUFFICIENT_STOCK");
        assertThat(orderRepository.count()).isZero();
        assertThat(outboxMessageRepository.count()).isZero();
    }

    @Test
    void mapMissingProductWithoutSavingOrder() {
        String accessToken = createUserAndAccessToken("missing-product-user");
        UUID productId = UUID.randomUUID();
        when(inventoryClient.checkAvailability(productId, 1))
                .thenThrow(new ProductNotFoundException(productId));

        ResponseEntity<String> response = createOrder(
                accessToken,
                orderBody(item(productId, 1))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("PRODUCT_NOT_FOUND");
        assertThat(orderRepository.count()).isZero();
        assertThat(outboxMessageRepository.count()).isZero();
    }

    @Test
    void mapUnavailableInventoryWithoutSavingOrder() {
        String accessToken = createUserAndAccessToken("unavailable-inventory-user");
        UUID productId = UUID.randomUUID();
        when(inventoryClient.checkAvailability(productId, 1))
                .thenThrow(new InventoryUnavailableException());

        ResponseEntity<String> response = createOrder(
                accessToken,
                orderBody(item(productId, 1))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("INVENTORY_UNAVAILABLE");
        assertThat(orderRepository.count()).isZero();
        assertThat(outboxMessageRepository.count()).isZero();
    }

    @Test
    void mapInventoryTimeoutWithoutSavingOrder() {
        String accessToken = createUserAndAccessToken("inventory-timeout-user");
        UUID productId = UUID.randomUUID();
        when(inventoryClient.checkAvailability(productId, 1))
                .thenThrow(new InventoryTimeoutException());

        ResponseEntity<String> response = createOrder(
                accessToken,
                orderBody(item(productId, 1))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).contains("INVENTORY_TIMEOUT");
        assertThat(orderRepository.count()).isZero();
        assertThat(outboxMessageRepository.count()).isZero();
    }

    @Test
    void rejectDuplicateProductBeforeInventoryCall() {
        String accessToken = createUserAndAccessToken("duplicate-product-user");
        UUID productId = UUID.randomUUID();

        ResponseEntity<String> response = createOrder(
                accessToken,
                orderBody(item(productId, 1), item(productId, 2))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("DUPLICATE_PRODUCT");
        verify(inventoryClient, never()).checkAvailability(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt()
        );
        assertThat(orderRepository.count()).isZero();
        assertThat(outboxMessageRepository.count()).isZero();
    }

    private String createUserAndAccessToken(String username) {
        User user = userRepository.saveAndFlush(new User(
                username,
                passwordEncoder.encode("strong-password"),
                username + "@example.com",
                UserRole.USER
        ));
        return jwtService.issueTokenPair(user.getId()).getAccessToken();
    }

    private ResponseEntity<String> createOrder(String accessToken, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        return restTemplate.exchange(
                "http://localhost:" + port + "/api/order",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );
    }

    @SafeVarargs
    private Map<String, Object> orderBody(Map<String, Object>... items) {
        return Map.of("items", List.of(items));
    }

    private Map<String, Object> item(UUID productId, int quantity) {
        return Map.of(
                "productId", productId.toString(),
                "quantity", quantity
        );
    }

    private ProductSnapshot product(
            UUID productId,
            String name,
            int availableQuantity,
            String price,
            String sale,
            boolean available
    ) {
        return new ProductSnapshot(
                productId,
                name,
                availableQuantity,
                new BigDecimal(price),
                new BigDecimal(sale),
                available
        );
    }
}
