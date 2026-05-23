package dev.caixadev.domain.port.out;

import dev.caixadev.domain.model.UserProjectRole;

import java.util.List;

public interface UserProjectRoleRepository {
    List<UserProjectRole> findByUserId(Long userId);
    void assign(Long userId, Long projectId, Long roleId);
    void remove(Long userId, Long projectId, Long roleId);
}

