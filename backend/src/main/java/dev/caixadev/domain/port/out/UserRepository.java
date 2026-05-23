package dev.caixadev.domain.port.out;

import dev.caixadev.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    /** Liste les utilisateurs réguliers (exclut les SuperAdmins). */
    List<User> findAllNonSuperAdmin();
    User save(User user);
    void deleteById(Long id);
    boolean existsByEmail(String email);
}

