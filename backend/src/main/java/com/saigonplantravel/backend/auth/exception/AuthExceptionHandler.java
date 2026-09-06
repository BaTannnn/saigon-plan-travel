package com.saigonplantravel.backend.auth.exception;

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
public class AuthExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException exception, HttpServletRequest request) {
        return ApiProblemDetails.create(
                HttpStatus.CONFLICT, "Email already exists", exception.getMessage(), "EMAIL_ALREADY_EXISTS", request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception, HttpServletRequest request) {
        return ApiProblemDetails.create(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed",
                exception.getMessage(),
                "INVALID_CREDENTIALS",
                request);
    }
}
