package ru.pavelshadrin.shop.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import ru.pavelshadrin.shop.order.support.PostgreSqlIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderServiceApplicationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    @Test
    void applicationStartsWithItsOwnNameAndTestProfile() {
        assertThat(context.getBean(OrderServiceApplication.class)).isNotNull();
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("order-service");
        assertThat(environment.getActiveProfiles()).contains("test");
        assertThat(port).isPositive();
    }

    @Test
    void knownRequestFieldsAreAccepted() throws Exception {
        ProbeRequest request = objectMapper.readValue("{\"productId\":\"example-id\"}", ProbeRequest.class);

        assertThat(request.productId()).isEqualTo("example-id");
    }

    @Test
    void unknownRequestFieldsAreRejected() {
        assertThatThrownBy(() -> objectMapper.readValue(
                "{\"productId\":\"example-id\",\"price\":100}", ProbeRequest.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }

    private record ProbeRequest(String productId) {
    }
}
