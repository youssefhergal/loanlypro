package com.projetfilrouge.loanmanagement.web.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<ApiError.FieldDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldDetail)
                .toList();
        ApiError body = ApiError.builder()
                .code("VALIDATION_ERROR")
                .message("Données invalides")
                .details(details)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException e) {
        return buildError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentification invalide");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException e) {
        return buildError(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenOperationException e) {
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException e) {
        return buildError(HttpStatus.BAD_REQUEST, "BUSINESS_RULE", e.getMessage());
    }

    @ExceptionHandler(LoanStorageException.class)
    public ResponseEntity<ApiError> handleLoanStorage(LoanStorageException e) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException e) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erreur interne du serveur");
    }

    private ResponseEntity<ApiError> buildError(HttpStatus status, String code, String message) {
        ApiError body = ApiError.builder()
                .code(code)
                .message(message)
                .details(List.of())
                .build();
        return ResponseEntity.status(status).body(body);
    }

    private ApiError.FieldDetail toFieldDetail(FieldError fieldError) {
        return ApiError.FieldDetail.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .build();
    }
}
