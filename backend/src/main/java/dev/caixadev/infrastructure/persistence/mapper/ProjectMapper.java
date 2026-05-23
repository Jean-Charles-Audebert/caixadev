package dev.caixadev.infrastructure.persistence.mapper;

import dev.caixadev.domain.model.Project;
import dev.caixadev.infrastructure.persistence.entity.ProjectEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    // toDomain : source = bean property (getter isActive() → "active", isPublic() → "public")
    //            target = record component name
    @Mapping(source = "active", target = "isActive")
    @Mapping(source = "public", target = "isPublic")
    Project toDomain(ProjectEntity entity);

    // toEntity : source = record component, target = builder method (même nom "isActive", "isPublic")
    // pas de @Mapping explicite nécessaire pour ces champs
    ProjectEntity toEntity(Project domain);
}
