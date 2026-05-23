package dev.caixadev.infrastructure.persistence.mapper;

import dev.caixadev.domain.model.User;
import dev.caixadev.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "password", target = "passwordHash")
    @Mapping(source = "superAdmin", target = "isSuperAdmin")
    User toDomain(UserEntity entity);

    @Mapping(source = "passwordHash", target = "password")
    // isSuperAdmin : même nom dans le record et dans le builder Lombok → pas besoin de @Mapping explicite
    UserEntity toEntity(User domain);
}
