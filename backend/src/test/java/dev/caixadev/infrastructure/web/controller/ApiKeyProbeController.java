package dev.caixadev.infrastructure.web.controller;

import dev.caixadev.infrastructure.security.ApiKeyPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur présent uniquement dans les sources de test.
 *
 * Il occupe un chemin de la chaîne de sécurité « clé API »
 * ({@code /cvc-gaines/**}) afin de tester cette chaîne indépendamment du
 * moteur de calcul cvc-gaines. Il renvoie le préfixe de la clé authentifiée,
 * ce qui prouve que l'identité vient bien de la clé et non d'un cookie.
 */
@RestController
class ApiKeyProbeController {

    @GetMapping("/cvc-gaines/v1/_probe")
    String probe(@AuthenticationPrincipal ApiKeyPrincipal principal) {
        return principal == null ? "anonymous" : principal.keyPrefix();
    }
}
