package com.nc.sinpase.poc.modulith.covoit;

import com.nc.sinpase.poc.modulith.covoit.auth.InvalidCredentialsException;
import com.nc.sinpase.poc.modulith.covoit.auth.InvalidTokenException;
import com.nc.sinpase.poc.modulith.covoit.bookings.BookingNotFoundException;
import com.nc.sinpase.poc.modulith.covoit.bookings.InvalidStatusTransitionException;
import com.nc.sinpase.poc.modulith.covoit.identity.UserAlreadyExistsException;
import com.nc.sinpase.poc.modulith.covoit.rides.NoSeatsAvailableException;
import com.nc.sinpase.poc.modulith.covoit.rides.RideNotBookableException;
import com.nc.sinpase.poc.modulith.covoit.rides.RideNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ErrorResponse.of("USER_ALREADY_EXISTS", ex.getMessage(), traceId());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentials(InvalidCredentialsException ex) {
        return ErrorResponse.of("UNAUTHENTICATED", ex.getMessage(), traceId());
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidToken(InvalidTokenException ex) {
        return ErrorResponse.of("UNAUTHENTICATED", ex.getMessage(), traceId());
    }

    @ExceptionHandler(RideNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleRideNotFound(RideNotFoundException ex) {
        return ErrorResponse.of("NOT_FOUND", ex.getMessage(), traceId());
    }

    @ExceptionHandler(BookingNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleBookingNotFound(BookingNotFoundException ex) {
        return ErrorResponse.of("NOT_FOUND", ex.getMessage(), traceId());
    }

    @ExceptionHandler(RideNotBookableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleRideNotBookable(RideNotBookableException ex) {
        return ErrorResponse.of("RIDE_NOT_BOOKABLE", ex.getMessage(), traceId());
    }

    @ExceptionHandler(NoSeatsAvailableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleNoSeats(NoSeatsAvailableException ex) {
        return ErrorResponse.of("NO_SEATS_AVAILABLE", ex.getMessage(), traceId());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidTransition(InvalidStatusTransitionException ex) {
        return ErrorResponse.of("INVALID_STATUS_TRANSITION", ex.getMessage(), traceId());
    }

    @ExceptionHandler(ConcurrentUpdateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConcurrentUpdate(ConcurrentUpdateException ex) {
        return ErrorResponse.of("CONCURRENT_UPDATE", ex.getMessage(), traceId());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(ForbiddenException ex) {
        return ErrorResponse.of("FORBIDDEN", ex.getMessage(), traceId());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a
                ));
        return ErrorResponse.of("VALIDATION_ERROR", "Validation failed", details, traceId());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        return ErrorResponse.of("VALIDATION_ERROR", ex.getMessage(), traceId());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return ErrorResponse.of("FORBIDDEN", "Access denied", traceId());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        log.error("Unexpected error [traceId={}]", traceId(), ex);
        return ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred", traceId());
    }

    private String traceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "unknown";
    }
}
