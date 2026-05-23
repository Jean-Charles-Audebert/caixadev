package dev.caixadev.domain.service;

import dev.caixadev.domain.exception.ProjectNotFoundException;
import dev.caixadev.domain.exception.RoleNotFoundException;
import dev.caixadev.domain.exception.UserAlreadyExistsException;
import dev.caixadev.domain.exception.UserNotFoundException;
import dev.caixadev.domain.model.User;
import dev.caixadev.domain.port.in.UserUseCase;
import dev.caixadev.domain.port.out.PasswordHashPort;
import dev.caixadev.domain.port.out.ProjectRepository;
import dev.caixadev.domain.port.out.RoleRepository;
import dev.caixadev.domain.port.out.UserProjectRoleRepository;
import dev.caixadev.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final RoleRepository roleRepository;
    private final UserProjectRoleRepository userProjectRoleRepository;
    private final PasswordHashPort passwordHashPort;

    @Override
    public List<User> listUsers() {
        return userRepository.findAllNonSuperAdmin();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .filter(u -> !u.isSuperAdmin())
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional
    public User createUser(String email, String password, String displayName) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }
        User user = new User(null, email, passwordHashPort.encode(password), displayName, false, null);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUser(Long id, String displayName) {
        User existing = getUserById(id);
        User updated = new User(
                existing.id(), existing.email(), existing.passwordHash(),
                displayName, existing.isSuperAdmin(), existing.createdAt()
        );
        return userRepository.save(updated);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        getUserById(id); // vérifie l'existence et que ce n'est pas un SuperAdmin
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void assignRole(Long userId, Long projectId, Long roleId) {
        getUserById(userId);
        projectRepository.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
        roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));
        userProjectRoleRepository.assign(userId, projectId, roleId);
    }

    @Override
    @Transactional
    public void removeRole(Long userId, Long projectId, Long roleId) {
        userProjectRoleRepository.remove(userId, projectId, roleId);
    }
}

