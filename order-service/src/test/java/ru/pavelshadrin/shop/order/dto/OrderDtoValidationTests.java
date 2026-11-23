package ru.pavelshadrin.shop.order.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import ru.pavelshadrin.shop.order.dto.order.CreateOrderItemRequest;
import ru.pavelshadrin.shop.order.dto.order.CreateOrderRequest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDtoValidationTests {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();

    private final Validator validator = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void acceptValidOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderItemRequest(UUID.randomUUID(), 2)
        ));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectNullOrEmptyItems() {
        assertThat(validator.validate(new CreateOrderRequest(null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("items");
        assertThat(validator.validate(new CreateOrderRequest(List.of())))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("items");
    }

    @Test
    void rejectInvalidNestedItem() {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderItemRequest(null, 0)
        ));

        Set<String> fields = validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder(
                "items[0].productId",
                "items[0].quantity"
        );
    }
}
