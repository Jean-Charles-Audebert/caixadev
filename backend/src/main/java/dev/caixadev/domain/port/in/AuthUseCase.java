package dev.caixadev.domain.port.in;

import dev.caixadev.domain.model.LoginResult;

public interface AuthUseCase {
    /** Authentifie l'utilisateur et retourne les tokens (accès + refresh). */
    LoginResult login(String email, String password);

    /** Renouvelle la paire de tokens à partir du refresh token brut. */
    LoginResult refresh(String rawRefreshToken);

    /** Révoque le refresh token (déconnexion). */
    void logout(String rawRefreshToken);
}

