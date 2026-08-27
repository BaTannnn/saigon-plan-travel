package com.saigonplantravel.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Set<String> PAGINATION_FIELDS = Set.of("page", "size");

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        boolean paginationField = PAGINATION_FIELDS.contains(exception.getName());

        ProblemDetail problem = ApiProblemDetails.create(
                HttpStatus.BAD_REQUEST,
                paginationField ? "Invalid pagination parameters" : "Invalid request",
                paginationField ? "page and size must be valid integers" : "Request validation failed",
                "INVALID_REQUEST",
                request);
        problem.setProperty("fieldErrors", List.of(new FieldValidationError(exception.getName(), "has invalid type")));

        return problem;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingRequestParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return validationProblem(
                List.of(new FieldValidationError(exception.getParameterName(), "is required")), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ProblemDetail handleValidation(Exception exception, HttpServletRequest request) {
        BindingResult bindingResult = exception instanceof MethodArgumentNotValidException invalid
                ? invalid.getBindingResult()
                : ((BindException) exception).getBindingResult();

        List<FieldValidationError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(this::toFieldValidationError)
                .sorted(Comparator.comparing(FieldValidationError::field))
                .toList();
        return validationProblem(fieldErrors, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidation(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        List<FieldValidationError> fieldErrors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldValidationError(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .sorted(Comparator.comparing(FieldValidationError::field))
                .toList();
        return validationProblem(fieldErrors, request);
    }

    private ProblemDetail validationProblem(List<FieldValidationError> fieldErrors, HttpServletRequest request) {
        boolean paginationOnly = !fieldErrors.isEmpty()
                && fieldErrors.stream().allMatch(error -> PAGINATION_FIELDS.contains(error.field()));

        String detail = paginationOnly && fieldErrors.size() == 1
                ? fieldErrors.getFirst().message()
                : "Request validation failed";
        ProblemDetail problem = ApiProblemDetails.create(
                HttpStatus.BAD_REQUEST,
                paginationOnly ? "Invalid pagination parameters" : "Invalid request",
                detail,
                "INVALID_REQUEST",
                request);
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return ApiProblemDetails.create(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "Request body is malformed or contains an invalid value",
                "INVALID_REQUEST",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error while handling {}", request.getRequestURI(), exception);

        return ApiProblemDetails.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred",
                "INTERNAL_SERVER_ERROR",
                request);
    }

    private FieldValidationError toFieldValidationError(FieldError fieldError) {
        String message;
        if (fieldError.isBindingFailure() && PAGINATION_FIELDS.contains(fieldError.getField())) {
            message = "page and size must be valid integers";
        } else if (fieldError.isBindingFailure()) {
            message = fieldError.getField() + " has invalid type";
        } else {
            message = fieldError.getDefaultMessage();
        }
        return new FieldValidationError(fieldError.getField(), message);
    }

    private record FieldValidationError(String field, String message) {}
}
