package com.saigonplantravel.backend.place.exception;

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
public class PlaceExceptionHandler {

    @ExceptionHandler(PlaceNotFoundException.class)
    public ProblemDetail handlePlaceNotFound(PlaceNotFoundException exception, HttpServletRequest request) {
        return ApiProblemDetails.create(
                HttpStatus.NOT_FOUND, "Place not found", exception.getMessage(), "PLACE_NOT_FOUND", request);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException exception, HttpServletRequest request) {
        return ApiProblemDetails.create(
                HttpStatus.NOT_FOUND, "Category not found", exception.getMessage(), "CATEGORY_NOT_FOUND", request);
    }
}
