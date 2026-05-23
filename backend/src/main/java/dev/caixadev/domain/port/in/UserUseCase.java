package dev.caixadev.domain.port.in;

import dev.caixadev.domain.model.User;

import java.util.List;

public interface UserUseCase {
    List<User> listUsers();
    User getUserById(Long id);
    User createUser(String email, String password, String displayName);
    User updateUser(Long id, String displayName);
    void deleteUser(Long id);
    void assignRole(Long userId, Long projectId, Long roleId);
    void removeRole(Long userId, Long projectId, Long roleId);
}

