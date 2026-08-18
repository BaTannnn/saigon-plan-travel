package com.saigonplantravel.backend.common.exception;

import com.saigonplantravel.backend.auth.exception.EmailAlreadyExistsException;
import com.saigonplantravel.backend.auth.exception.InvalidCredentialsException;
import com.saigonplantravel.backend.itinerary.exception.*;
import com.saigonplantravel.backend.place.exception.CategoryNotFoundException;
import com.saigonplantravel.backend.place.exception.PlaceNotFoundException;
import com.saigonplantravel.backend.trip.exception.InvalidTripException;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI ABOUT_BLANK = URI.create("about:blank");
    private static final Set<String> PAGINATION_FIELDS = Set.of("page", "size");

    @ExceptionHandler(InvalidPaginationException.class)
    public ProblemDetail handleInvalidPagination(InvalidPaginationException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());

        problem.setType(ABOUT_BLANK);
        problem.setTitle("Invalid pagination parameters");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_REQUEST");

        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        boolean paginationField = PAGINATION_FIELDS.contains(exception.getName());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                paginationField ? "page and size must be valid integers" : "Request validation failed");

        problem.setType(ABOUT_BLANK);
        problem.setTitle(paginationField ? "Invalid pagination parameters" : "Invalid request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_REQUEST");
        problem.setProperty("fieldErrors", List.of(new FieldValidationError(exception.getName(), "has invalid type")));

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
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(ABOUT_BLANK);
        problem.setTitle(paginationOnly ? "Invalid pagination parameters" : "Invalid request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_REQUEST");
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request body is malformed or contains an invalid value");

        problem.setType(ABOUT_BLANK);
        problem.setTitle("Invalid request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_REQUEST");

        return problem;
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());

        problem.setTitle("Email already exists");

        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());

        problem.setTitle("Authentication failed");

        return problem;
    }

    @ExceptionHandler(InvalidTripException.class)
    public ProblemDetail handleInvalidTrip(InvalidTripException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());

        problem.setType(ABOUT_BLANK);
        problem.setTitle("Invalid request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_REQUEST");
        problem.setProperty(
                "fieldErrors", List.of(new FieldValidationError(exception.getField(), exception.getMessage())));

        return problem;
    }

    @ExceptionHandler(TripNotFoundException.class)
    public ProblemDetail handleTripNotFound(TripNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());

        problem.setType(ABOUT_BLANK);
        problem.setTitle("Trip not found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "TRIP_NOT_FOUND");

        return problem;
    }

    @ExceptionHandler(PlaceNotFoundException.class)
    public ProblemDetail handlePlaceNotFound(PlaceNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(ABOUT_BLANK);
        problem.setTitle("Place not found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "PLACE_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(ABOUT_BLANK);
        problem.setTitle("Category not found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "CATEGORY_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(ItineraryItemNotFoundException.class)
    public ProblemDetail handleItineraryItemNotFound(
            ItineraryItemNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(ABOUT_BLANK);
        problem.setTitle("Itinerary item not found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "ITINERARY_ITEM_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(DuplicateItineraryPlaceException.class)
    public ProblemDetail handleDuplicateItineraryPlace(
            DuplicateItineraryPlaceException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(ABOUT_BLANK);
        problem.setTitle("Duplicate itinerary Place");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "DUPLICATE_ITINERARY_PLACE");
        return problem;
    }

    @ExceptionHandler(InactiveItineraryPlaceException.class)
    public ProblemDetail handleInactiveItineraryPlace(
            InactiveItineraryPlaceException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(ABOUT_BLANK);
        problem.setTitle("Inactive itinerary Place");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "ITINERARY_PLACE_INACTIVE");
        return problem;
    }

    @ExceptionHandler(InvalidItineraryOrderException.class)
    public ProblemDetail handleInvalidItineraryOrder(
            InvalidItineraryOrderException exception, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());

        problem.setTitle("Invalid itinerary order");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_ITINERARY_ORDER");

        return problem;
    }

    @ExceptionHandler(InvalidGeneratedItineraryException.class)
    public ProblemDetail handleInvalidGeneratedItinerary(InvalidGeneratedItineraryException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());

        problem.setTitle("Invalid generated itinerary");

        problem.setProperty("code", "INVALID_GENERATED_ITINERARY");

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error while handling {}", request.getRequestURI(), exception);

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
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

    private record FieldValidationError(String field, String message) {}
}
