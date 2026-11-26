package dev.caixadev.tools.cvcgaines.api;

import dev.caixadev.application.dto.response.ApiErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erreurs de saisie du moteur (valeur hors domaine, reseau incoherent,
 * aucune dimension normalisee...) : 400 avec le message du moteur, redige
 * pour l'utilisateur et sans detail interne.
 *
 * <p>Limite aux controleurs de {@code cvc-gaines} : ailleurs, une
 * {@link IllegalArgumentException} reste une erreur interne (500) dont le
 * message n'est pas expose.
 */
@RestControllerAdvice(assignableTypes = CvcGainesController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CvcGainesExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidInput(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("CALCULATION_INPUT_ERROR", ex.getMessage()));
    }
}
