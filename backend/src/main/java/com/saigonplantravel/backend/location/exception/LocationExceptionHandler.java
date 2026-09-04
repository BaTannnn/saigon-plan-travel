package com.saigonplantravel.backend.location.exception;

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
public class LocationExceptionHandler {

    @ExceptionHandler(GeocodingUnavailableException.class)
    public ProblemDetail handleGeocodingUnavailable(
            GeocodingUnavailableException exception, HttpServletRequest request) {
        return ApiProblemDetails.create(
                HttpStatus.BAD_GATEWAY,
                "Location search unavailable",
                "Location search is temporarily unavailable",
                "GEOCODING_UNAVAILABLE",
                request);
    }
}
