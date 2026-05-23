-- =========================
-- USERS
-- =========================
CREATE TABLE users (
    id           BIGSERIAL    PRIMARY KEY,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    is_super_admin BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- PROJECTS
-- =========================
CREATE TABLE projects (
    id          BIGSERIAL    PRIMARY KEY,
    key         VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_public   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- ROLES
-- =========================
CREATE TABLE roles (
    id         BIGSERIAL  PRIMARY KEY,
    project_id BIGINT     NULL,
    name       VARCHAR(100) NOT NULL,

    CONSTRAINT fk_roles_project
        FOREIGN KEY (project_id)
        REFERENCES projects(id)
        ON DELETE CASCADE
);

-- Rôles globaux (project_id IS NULL) : nom unique
CREATE UNIQUE INDEX uk_roles_global_name
    ON roles (name)
    WHERE project_id IS NULL;

-- Rôles liés à un projet : (project_id, name) unique
CREATE UNIQUE INDEX uk_roles_project_name
    ON roles (project_id, name)
    WHERE project_id IS NOT NULL;

-- =========================
-- PERMISSIONS
-- =========================
CREATE TABLE permissions (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(150) NOT NULL UNIQUE,
    description TEXT
);

-- =========================
-- ROLE_PERMISSIONS (pivot)
-- =========================
CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_rp_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,

    CONSTRAINT fk_rp_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE INDEX idx_rp_role       ON role_permissions(role_id);
CREATE INDEX idx_rp_permission ON role_permissions(permission_id);

-- =========================
-- USER_PROJECT_ROLES (pivot)
-- =========================
CREATE TABLE user_project_roles (
    user_id    BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    role_id    BIGINT NOT NULL,

    PRIMARY KEY (user_id, project_id, role_id),

    CONSTRAINT fk_upr_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_upr_project
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,

    CONSTRAINT fk_upr_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_upr_user    ON user_project_roles(user_id);
CREATE INDEX idx_upr_project ON user_project_roles(project_id);

-- =========================
-- REFRESH_TOKENS
-- =========================
CREATE TABLE refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rt_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_rt_user ON refresh_tokens(user_id);

-- =========================
-- PASSWORD_RESET_TOKENS
-- =========================
CREATE TABLE password_reset_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_prt_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
