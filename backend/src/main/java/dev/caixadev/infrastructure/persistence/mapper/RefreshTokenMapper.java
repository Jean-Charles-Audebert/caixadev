package dev.caixadev.infrastructure.persistence.mapper;

import dev.caixadev.domain.model.RefreshToken;
import dev.caixadev.infrastructure.persistence.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {

    @Mapping(source = "user.id", target = "userId")
    RefreshToken toDomain(RefreshTokenEntity entity);
}

