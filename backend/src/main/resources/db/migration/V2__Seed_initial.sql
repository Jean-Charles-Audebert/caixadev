-- =========================
-- SEED : Rôles globaux
-- =========================
INSERT INTO roles (name) VALUES
    ('ADMIN'),
    ('MEMBER'),
    ('BOARD'),
    ('PLAYER'),
    ('CALLER'),
    ('GUEST'),
    ('PREMIUM');

-- =========================
-- SEED : Projet de test (dev uniquement)
-- =========================
INSERT INTO projects (key, name, description, is_active, is_public)
VALUES ('test', 'Projet Test', 'Projet de démonstration', TRUE, TRUE);

INSERT INTO roles (project_id, name)
VALUES
    ((SELECT id FROM projects WHERE key = 'test'), 'ADMIN'),
    ((SELECT id FROM projects WHERE key = 'test'), 'MEMBER');

