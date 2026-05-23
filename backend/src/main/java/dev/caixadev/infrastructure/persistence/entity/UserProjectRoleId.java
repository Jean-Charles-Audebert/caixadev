package dev.caixadev.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserProjectRoleId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "role_id")
    private Long roleId;
}

