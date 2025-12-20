package ru.practicum.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MissingRequestHeaderException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            WrongTimeException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestException(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Incorrectly made request.", exception, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalServerError(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", exception, request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus httpStatus, String message, Exception exception, HttpServletRequest request) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String status = httpStatus.name();
        String reason = httpStatus.getReasonPhrase();
        String errorMessage = exception.getMessage();

        String logMessage = String.format("%s %s - %s %s: %s",
                request.getMethod(), request.getRequestURI(),
                httpStatus.value(), status, errorMessage);

        if (httpStatus.is5xxServerError()) {
            log.error(logMessage, exception);
        } else {
            log.warn(logMessage);
        }

        return ResponseEntity
                .status(httpStatus)
                .body(new ErrorResponse(status, reason, message, timestamp));
    }
}