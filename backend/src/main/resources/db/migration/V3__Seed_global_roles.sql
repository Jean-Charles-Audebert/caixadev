-- =========================
-- Référentiel des rôles globaux (tous environnements)
-- =========================
-- Les rôles globaux sont nécessaires au fonctionnement de l'application
-- (assignation de rôles, administration). Ce ne sont pas des données de
-- démonstration : ils sont donc créés en production comme en développement.
--
-- L'instruction est idempotente : sur une base de développement où
-- V2__Seed_initial.sql (désormais dans db/seed) a déjà inséré ces rôles,
-- aucune ligne n'est ajoutée.
INSERT INTO roles (name) VALUES
    ('ADMIN'),
    ('MEMBER'),
    ('BOARD'),
    ('PLAYER'),
    ('CALLER'),
    ('GUEST'),
    ('PREMIUM')
ON CONFLICT DO NOTHING;
