package com.saigonplantravel.backend.itinerary.exception;

import com.saigonplantravel.backend.common.exception.ApiProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(annotations = RestController.class)
public class ItineraryExceptionHandler {

    @ExceptionHandler(ItineraryItemNotFoundException.class)
    public ProblemDetail handleItineraryItemNotFound(
            ItineraryItemNotFoundException exception, HttpServletRequest request) {
        return ApiProblemDetails.create(
                HttpStatus.NOT_FOUND,
                "Itinerary item not found",
                exception.getMessage(),
                "ITINERARY_ITEM_NOT_FOUND",
                request);
    }

    @ExceptionHandler(DuplicateItineraryPlaceException.class)
    public ProblemDetail handleDuplicateItineraryPlace(
            DuplicateItineraryPlaceException exception, HttpServletRequest request) {
        return ApiProblemDetails.create(
                HttpStatus.CONFLICT,
                "Duplicate itinerary Place",
                exception.getMessage(),
                "DUPLICATE_ITINERARY_PLACE",
                request);
    }

    @ExceptionHandler(InactiveItineraryPlaceException.class)
    public ProblemDetail handleInactiveItineraryPlace(
            InactiveItineraryPlaceException exception, HttpServletRequest request) {
        return ApiProblemDetails.create(
                HttpStatus.CONFLICT,
                "Inactive itinerary Place",
                exception.getMessage(),
                "ITINERARY_PLACE_INACTIVE",
                request);
    }

    @ExceptionHandler(InvalidItineraryOrderException.class)
    public ProblemDetail handleInvalidItineraryOrder(
            InvalidItineraryOrderException exception, HttpServletRequest request) {
        return ApiProblemDetails.create(
                HttpStatus.BAD_REQUEST,
                "Invalid itinerary order",
                exception.getMessage(),
                "INVALID_ITINERARY_ORDER",
                request);
    }

    @ExceptionHandler(InvalidGeneratedItineraryException.class)
    public ProblemDetail handleInvalidGeneratedItinerary(
            InvalidGeneratedItineraryException exception, HttpServletRequest request) {
        return ApiProblemDetails.create(
                HttpStatus.BAD_REQUEST,
                "Invalid generated itinerary",
                exception.getMessage(),
                "INVALID_GENERATED_ITINERARY",
                request);
    }
}
