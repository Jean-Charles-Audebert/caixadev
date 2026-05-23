package dev.caixadev.infrastructure.persistence.mapper;

import dev.caixadev.domain.model.Role;
import dev.caixadev.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(source = "project.id", target = "projectId")
    Role toDomain(RoleEntity entity);
}

