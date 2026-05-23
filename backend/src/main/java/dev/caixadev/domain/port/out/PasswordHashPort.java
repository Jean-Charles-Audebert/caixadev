package dev.caixadev.domain.port.out;

public interface PasswordHashPort {
    boolean matches(String rawPassword, String hashedPassword);
    String encode(String rawPassword);
}

