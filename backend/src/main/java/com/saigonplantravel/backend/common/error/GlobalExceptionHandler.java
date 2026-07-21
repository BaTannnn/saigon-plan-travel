package com.saigonplantravel.backend.common.error;

import com.saigonplantravel.backend.place.exception.PlaceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI ABOUT_BLANK = URI.create("about:blank");
    private static final Set<String> PAGINATION_FIELDS = Set.of("page", "size");

    @ExceptionHandler({
            InvalidPaginationException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ProblemDetail handleBadRequest(Exception exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception instanceof InvalidPaginationException
                        ? exception.getMessage()
                        : "page and size must be valid integers"
        );
        problem.setType(ABOUT_BLANK);
        problem.setTitle("Invalid pagination parameters");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_REQUEST");
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            problem.setProperty(
                    "fieldErrors",
                    List.of(new FieldValidationError(mismatch.getName(), "has invalid type"))
            );
        }
        return problem;
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
        boolean paginationOnly = !fieldErrors.isEmpty()
                && fieldErrors.stream().allMatch(error -> PAGINATION_FIELDS.contains(error.field()));

        String detail = paginationOnly && fieldErrors.size() == 1
                ? fieldErrors.getFirst().message()
                : "Request validation failed";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(ABOUT_BLANK);
        problem.setTitle(paginationOnly ? "Invalid pagination parameters" : "Invalid request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_REQUEST");
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(PlaceNotFoundException.class)
    public ProblemDetail handlePlaceNotFound(
            PlaceNotFoundException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
        problem.setType(ABOUT_BLANK);
        problem.setTitle("Place not found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "PLACE_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error while handling {}", request.getRequestURI(), exception);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problem.setType(ABOUT_BLANK);
        problem.setTitle("Internal server error");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
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

    private record FieldValidationError(String field, String message) {
    }
}
