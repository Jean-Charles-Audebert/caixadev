package dev.caixadev.infrastructure.web.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur présent uniquement dans les sources de test.
 *
 * Il est volontairement placé hors de `/api/admin`, donc hors de la règle
 * d'URL de {@code SecurityConfig} : seul {@code @PreAuthorize} peut le
 * protéger. Cela permet de vérifier que la couche MVC convertit bien
 * {@code AccessDeniedException} en 403 (et non en 500), indépendamment du
 * {@code RestAccessDeniedHandler} des filtres de sécurité.
 */
@RestController
@RequestMapping("/api/test/method-security")
@PreAuthorize("hasRole('SUPER_ADMIN')")
class MethodSecurityProbeController {

    @GetMapping
    String probe() {
        return "ok";
    }
}
