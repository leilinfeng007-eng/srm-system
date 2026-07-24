package com.srm.common.exception;

import com.srm.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode error = exception.errorCode();
        return ResponseEntity.status(error.status())
                .body(ApiResponse.error(error.code(), exception.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldViolation>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception) {
        List<FieldViolation> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        ErrorCode error = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(error.status())
                .body(ApiResponse.error(error.code(), error.defaultMessage(), details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<FieldViolation>>> handleConstraintViolation(
            ConstraintViolationException exception) {
        List<FieldViolation> details = exception.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        ErrorCode error = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(error.status())
                .body(ApiResponse.error(error.code(), error.defaultMessage(), details));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception exception) {
        ErrorCode error = ErrorCode.RESOURCE_NOT_FOUND;
        return ResponseEntity.status(error.status())
                .body(ApiResponse.error(error.code(), error.defaultMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled request failure", exception);
        ErrorCode error = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(error.code(), error.defaultMessage(), null));
    }

    private FieldViolation toViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage());
    }
}
