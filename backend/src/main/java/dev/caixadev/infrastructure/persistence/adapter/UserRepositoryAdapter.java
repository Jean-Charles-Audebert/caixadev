package dev.caixadev.infrastructure.persistence.adapter;

import dev.caixadev.domain.model.User;
import dev.caixadev.domain.port.out.UserRepository;
import dev.caixadev.infrastructure.persistence.entity.UserEntity;
import dev.caixadev.infrastructure.persistence.mapper.UserMapper;
import dev.caixadev.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;
    private final UserMapper mapper;

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<User> findAllNonSuperAdmin() {
        return jpa.findAllByIsSuperAdminFalse().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }
}

