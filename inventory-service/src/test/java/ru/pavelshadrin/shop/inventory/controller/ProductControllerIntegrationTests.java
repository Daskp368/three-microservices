package ru.pavelshadrin.shop.inventory.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.pavelshadrin.shop.inventory.repository.ProductRepository;
import ru.pavelshadrin.shop.inventory.support.PostgreSqlIntegrationTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
class ProductControllerIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void clearProducts() {
        productRepository.deleteAll();
    }

    @Test
    void productCrudLifecycleWorks() throws Exception {
        String createRequest = """
                {
                  "name": "Ноутбук",
                  "quantity": 10,
                  "price": 100000.00,
                  "sale": 5.00
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Ноутбук"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.price").value(100000.00))
                .andExpect(jsonPath("$.sale").value(5.00))
                .andReturn();

        JsonNode createdProduct = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String productId = createdProduct.get("id").asText();

        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Ноутбук"));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(productId));

        String updateRequest = """
                {
                  "name": "Ноутбук Pro",
                  "quantity": 7,
                  "price": 120000.50,
                  "sale": 10.00
                }
                """;

        mockMvc.perform(put("/api/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Ноутбук Pro"))
                .andExpect(jsonPath("$.quantity").value(7))
                .andExpect(jsonPath("$.price").value(120000.50))
                .andExpect(jsonPath("$.sale").value(10.00));

        mockMvc.perform(delete("/api/products/{productId}", productId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(productRepository.existsById(UUID.fromString(productId))).isFalse();
    }

    @Test
    void emptyProductListReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void invalidProductIdReturnsContractError() throws Exception {
        mockMvc.perform(get("/api/products/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_ID"))
                .andExpect(jsonPath("$.path").value("/api/products/not-a-uuid"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void invalidProductIdIsRejectedForUpdateAndDelete() throws Exception {
        String validRequest = """
                {
                  "name": "Товар",
                  "quantity": 1,
                  "price": 10.00,
                  "sale": 0.00
                }
                """;

        mockMvc.perform(put("/api/products/not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ID"))
                .andExpect(jsonPath("$.path").value("/api/products/not-a-uuid"));

        mockMvc.perform(delete("/api/products/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ID"))
                .andExpect(jsonPath("$.path").value("/api/products/not-a-uuid"));

        assertThat(productRepository.count()).isZero();
    }

    @Test
    void missingProductReturnsContractError() throws Exception {
        UUID missingProductId = UUID.randomUUID();

        mockMvc.perform(get("/api/products/{productId}", missingProductId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/products/" + missingProductId))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void invalidProductFieldsReturnFieldErrors() throws Exception {
        String invalidRequest = """
                {
                  "name": " ",
                  "quantity": -1,
                  "price": -0.01,
                  "sale": 100.01
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/products"))
                .andExpect(jsonPath("$.fieldErrors[*].field",
                        containsInAnyOrder("name", "quantity", "price", "sale")));
    }

    @Test
    void databaseSizedValuesAreRejectedBeforePersistence() throws Exception {
        String invalidRequest = """
                {
                  "name": "%s",
                  "quantity": 1,
                  "price": 100000000000000000.00,
                  "sale": 10.001
                }
                """.formatted("x".repeat(256));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[*].field",
                        containsInAnyOrder("name", "price", "sale")));

        assertThat(productRepository.count()).isZero();
    }

    @Test
    void unknownJsonFieldReturnsValidationError() throws Exception {
        String requestWithUnknownField = """
                {
                  "name": "Ноутбук",
                  "quantity": 10,
                  "price": 100000.00,
                  "sale": 5.00,
                  "unexpected": true
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithUnknownField))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Некорректное тело запроса"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }
}
