package com.roommate_connection_platform.customexceptions;

import com.roommate_connection_platform.ratelimit.LoginRateLimitException;
import com.roommate_connection_platform.ratelimit.LoginRateLimitResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final LoginRateLimitResponseFactory loginRateLimitResponses;

    @ExceptionHandler(LoginRateLimitException.class)
    public ResponseEntity<ApiError> handleLoginRateLimit(LoginRateLimitException ex) {
        return loginRateLimitResponses.create(ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> Optional.ofNullable(fieldError.getDefaultMessage()).orElse("Invalid value"),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Invalid request data",
                fieldErrors
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        return buildResponse(ex.getStatus(), ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ApiError> handleDataNotFound(DataNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "DATA_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidParamException.class)
    public ResponseEntity<ApiError> handleInvalidParam(InvalidParamException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_PARAM", ex.getMessage(), null);
    }

    @ExceptionHandler(PermissionDenyException.class)
    public ResponseEntity<ApiError> handlePermissionDeny(PermissionDenyException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), null);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String code, String message, Object details) {
        ApiError apiError = new ApiError(
                Instant.now(),
                status.value(),
                code,
                message,
                details
        );
        return ResponseEntity.status(status).body(apiError);
    }
}
