package dev.caixadev.infrastructure.web.advice;

import dev.caixadev.application.dto.response.ApiErrorResponse;
import dev.caixadev.domain.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of("UNAUTHORIZED", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of("INVALID_TOKEN", ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProjectNotFound(ProjectNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRoleNotFound(RoleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("VALIDATION_ERROR", details));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        // Cas d'un refus levé par la sécurité de méthode (@PreAuthorize) : sans ce
        // handler, le catch-all Exception le transformerait en 500 au lieu de 403.
        // Même format que RestAccessDeniedHandler, pour rester cohérent côté client.
        log.debug("Accès refusé : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of("FORBIDDEN", "Accès refusé"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        // Les détails (structure Jackson, positions) restent dans les journaux de debug,
        // jamais dans la réponse HTTP.
        log.debug("Corps de requête illisible : {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("MALFORMED_REQUEST", "Le corps de la requête est invalide"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        log.error("Erreur inattendue", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", "Une erreur interne est survenue"));
    }
}

