package com.saigonplantravel.backend.scheduling.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulingPropertiesTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory =
                Validation
                        .buildDefaultValidatorFactory();

        validator =
                validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsBaselineConfiguration() {
        SchedulingProperties properties =
                new SchedulingProperties(
                        "GREEDY_V1",
                        new BigDecimal("18.00"),
                        5,
                        100
                );

        Set<
                ConstraintViolation<
                        SchedulingProperties
                        >
                > violations =
                validator.validate(properties);

        assertThat(violations)
                .isEmpty();
    }

    @Test
    void rejectsInvalidSchedulingConfiguration() {
        SchedulingProperties properties =
                new SchedulingProperties(
                        "UNKNOWN",
                        BigDecimal.ZERO,
                        -1,
                        101
                );

        Set<
                ConstraintViolation<
                        SchedulingProperties
                        >
                > violations =
                validator.validate(properties);

        assertThat(violations)
                .extracting(
                        violation -> violation
                                .getPropertyPath()
                                .toString()
                )
                .containsExactlyInAnyOrder(
                        "algorithmVersion",
                        "averageSpeedKmh",
                        "fixedTransferMinutes",
                        "maxCandidates"
                );
    }
}