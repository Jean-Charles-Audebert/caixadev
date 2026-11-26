package dev.caixadev.infrastructure.persistence.adapter;

import dev.caixadev.domain.model.UserProjectRole;
import dev.caixadev.domain.port.out.UserProjectRoleRepository;
import dev.caixadev.infrastructure.persistence.entity.ProjectEntity;
import dev.caixadev.infrastructure.persistence.entity.RoleEntity;
import dev.caixadev.infrastructure.persistence.entity.UserEntity;
import dev.caixadev.infrastructure.persistence.entity.UserProjectRoleEntity;
import dev.caixadev.infrastructure.persistence.entity.UserProjectRoleId;
import dev.caixadev.infrastructure.persistence.mapper.ProjectMapper;
import dev.caixadev.infrastructure.persistence.mapper.RoleMapper;
import dev.caixadev.infrastructure.persistence.mapper.UserMapper;
import dev.caixadev.infrastructure.persistence.repository.ProjectJpaRepository;
import dev.caixadev.infrastructure.persistence.repository.RoleJpaRepository;
import dev.caixadev.infrastructure.persistence.repository.UserJpaRepository;
import dev.caixadev.infrastructure.persistence.repository.UserProjectRoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserProjectRoleRepositoryAdapter implements UserProjectRoleRepository {

    private final UserProjectRoleJpaRepository jpa;
    private final UserJpaRepository userJpa;
    private final ProjectJpaRepository projectJpa;
    private final RoleJpaRepository roleJpa;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final RoleMapper roleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserProjectRole> findByUserId(Long userId) {
        return jpa.findByUserId(userId).stream()
                .map(e -> new UserProjectRole(
                        userMapper.toDomain(e.getUser()),
                        projectMapper.toDomain(e.getProject()),
                        roleMapper.toDomain(e.getRole())
                ))
                .toList();
    }

    @Override
    public void assign(Long userId, Long projectId, Long roleId) {
        UserEntity user = userJpa.getReferenceById(userId);
        ProjectEntity project = projectJpa.getReferenceById(projectId);
        RoleEntity role = roleJpa.getReferenceById(roleId);
        UserProjectRoleId id = new UserProjectRoleId(userId, projectId, roleId);
        UserProjectRoleEntity entity = UserProjectRoleEntity.builder()
                .id(id).user(user).project(project).role(role).build();
        jpa.save(entity);
    }

    @Override
    public void remove(Long userId, Long projectId, Long roleId) {
        jpa.deleteById(new UserProjectRoleId(userId, projectId, roleId));
    }
}

