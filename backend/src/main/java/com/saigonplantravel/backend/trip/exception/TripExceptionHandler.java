package com.saigonplantravel.backend.trip.exception;

import com.saigonplantravel.backend.common.exception.ApiProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(annotations = RestController.class)
public class TripExceptionHandler {

    @ExceptionHandler(InvalidTripException.class)
    public ProblemDetail handleInvalidTrip(InvalidTripException exception, HttpServletRequest request) {
        ProblemDetail problem = ApiProblemDetails.create(
                HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), exception.getCode(), request);
        problem.setProperty(
                "fieldErrors", List.of(new FieldValidationError(exception.getField(), exception.getMessage())));

        return problem;
    }

    @ExceptionHandler(TripNotFoundException.class)
    public ProblemDetail handleTripNotFound(TripNotFoundException exception, HttpServletRequest request) {
        return ApiProblemDetails.create(
                HttpStatus.NOT_FOUND, "Trip not found", exception.getMessage(), "TRIP_NOT_FOUND", request);
    }

    private record FieldValidationError(String field, String message) {}
}
