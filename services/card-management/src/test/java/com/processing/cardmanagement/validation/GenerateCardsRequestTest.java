package com.processing.cardmanagement.validation;

import com.processing.common.dto.cardmanagement.GenerateCardsRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateCardsRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldRejectRequestWhenCountIsZero() {
        GenerateCardsRequest request = new GenerateCardsRequest(0, List.of("400000"));

        Set<ConstraintViolation<GenerateCardsRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("count");
    }

    @Test
    void shouldAcceptRequestWhenRequestIsValid() {
        GenerateCardsRequest request = new GenerateCardsRequest(100, List.of("400000", "400001"));

        assertThat(validator.validate(request)).isEmpty();
    }
}
