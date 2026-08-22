package com.saigonplantravel.backend.common.exception;

import com.saigonplantravel.backend.auth.exception.EmailAlreadyExistsException;
import com.saigonplantravel.backend.auth.exception.InvalidCredentialsException;
import com.saigonplantravel.backend.itinerary.exception.*;
import com.saigonplantravel.backend.location.exception.GeocodingUnavailableException;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI ABOUT_BLANK = URI.create("about:blank");
    private static final Set<String> PAGINATION_FIELDS = Set.of("page", "size");

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        boolean paginationField = PAGINATION_FIELDS.contains(exception.getName());

        ProblemDetail problem = problem(
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
        ProblemDetail problem = problem(
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
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "Request body is malformed or contains an invalid value",
                "INVALID_REQUEST",
                request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT, "Email already exists", exception.getMessage(), "EMAIL_ALREADY_EXISTS", request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed",
                exception.getMessage(),
                "INVALID_CREDENTIALS",
                request);
    }

    @ExceptionHandler(InvalidTripException.class)
    public ProblemDetail handleInvalidTrip(InvalidTripException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), exception.getCode(), request);
        problem.setProperty(
                "fieldErrors", List.of(new FieldValidationError(exception.getField(), exception.getMessage())));

        return problem;
    }

    @ExceptionHandler(GeocodingUnavailableException.class)
    public ProblemDetail handleGeocodingUnavailable(
            GeocodingUnavailableException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_GATEWAY,
                "Location search unavailable",
                "Location search is temporarily unavailable",
                "GEOCODING_UNAVAILABLE",
                request);
    }

    @ExceptionHandler(TripNotFoundException.class)
    public ProblemDetail handleTripNotFound(TripNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Trip not found", exception.getMessage(), "TRIP_NOT_FOUND", request);
    }

    @ExceptionHandler(PlaceNotFoundException.class)
    public ProblemDetail handlePlaceNotFound(PlaceNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Place not found", exception.getMessage(), "PLACE_NOT_FOUND", request);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.NOT_FOUND, "Category not found", exception.getMessage(), "CATEGORY_NOT_FOUND", request);
    }

    @ExceptionHandler(ItineraryItemNotFoundException.class)
    public ProblemDetail handleItineraryItemNotFound(
            ItineraryItemNotFoundException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Itinerary item not found",
                exception.getMessage(),
                "ITINERARY_ITEM_NOT_FOUND",
                request);
    }

    @ExceptionHandler(DuplicateItineraryPlaceException.class)
    public ProblemDetail handleDuplicateItineraryPlace(
            DuplicateItineraryPlaceException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "Duplicate itinerary Place",
                exception.getMessage(),
                "DUPLICATE_ITINERARY_PLACE",
                request);
    }

    @ExceptionHandler(InactiveItineraryPlaceException.class)
    public ProblemDetail handleInactiveItineraryPlace(
            InactiveItineraryPlaceException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "Inactive itinerary Place",
                exception.getMessage(),
                "ITINERARY_PLACE_INACTIVE",
                request);
    }

    @ExceptionHandler(InvalidItineraryOrderException.class)
    public ProblemDetail handleInvalidItineraryOrder(
            InvalidItineraryOrderException exception, HttpServletRequest request) {

        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid itinerary order",
                exception.getMessage(),
                "INVALID_ITINERARY_ORDER",
                request);
    }

    @ExceptionHandler(InvalidGeneratedItineraryException.class)
    public ProblemDetail handleInvalidGeneratedItinerary(
            InvalidGeneratedItineraryException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid generated itinerary",
                exception.getMessage(),
                "INVALID_GENERATED_ITINERARY",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error while handling {}", request.getRequestURI(), exception);

        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred",
                "INTERNAL_SERVER_ERROR",
                request);
    }

    private ProblemDetail problem(
            HttpStatus status, String title, String detail, String code, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(ABOUT_BLANK);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
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
