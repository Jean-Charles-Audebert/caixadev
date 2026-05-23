# Caixadev

## Démarrer localement

### Backend + PostgreSQL
```bash
cd backend
cp .env.example .env   # renseigner JWT_SECRET
docker-compose up -d   # PostgreSQL seulement
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev   # http://localhost:5173
```

### Stack complète (Docker)
```bash
docker-compose up -d --profile dev   # postgres + adminer + backend
```

---

## Suivi de la roadmap

### Phase 1 — Corrections ✅
- [x] `pom.xml` : JJWT 0.12.6 + MapStruct 1.6.3 ajoutés, name/description renseignés
- [x] `application.yml` : credentials externalisés via variables d'env, config JWT, profils dev/prod
- [x] `application-dev.yml` + `application-prod.yml` créés
- [x] `docker-compose.yml` : service backend ajouté, healthcheck postgres, `.env.example` créé
- [x] `V1__Schema_auth.sql` : commentaire corrigé, SUPERADMIN supprimé des rôles, UNIQUE index avec NULL, `display_name`, `is_public`, `refresh_tokens`, `password_reset_tokens`
- [x] `V2__Seed_initial.sql` : données de seed séparées du schéma

### Phase 2 — Point d'entrée ✅
- [x] `HomeApplication` : `scanBasePackages = "dev.caixadev"`, `@EnableJpaRepositories`, exclusion UserDetailsServiceAutoConfiguration

### Phase 3 — Domaine ✅
- [x] Modèles : `User`, `Project`, `Role`, `Permission`, `RefreshToken`, `TokenClaims`, `LoginResult`, `UserProjectRole`, `ProjectAccess`
- [x] Exceptions : `InvalidCredentialsException`, `InvalidTokenException`, `UserNotFoundException`, `UserAlreadyExistsException`, `ProjectNotFoundException`, `RoleNotFoundException`
- [x] Ports `in` : `AuthUseCase`, `UserUseCase`, `ProjectUseCase`
- [x] Ports `out` : `UserRepository`, `ProjectRepository`, `RoleRepository`, `RefreshTokenRepository`, `UserProjectRoleRepository`, `TokenPort`, `PasswordHashPort`

### Phase 4 — Infrastructure persistance ✅
- [x] Entités JPA : `UserEntity`, `ProjectEntity`, `RoleEntity`, `RefreshTokenEntity`, `PasswordResetTokenEntity`, `UserProjectRoleEntity` + `UserProjectRoleId`
- [x] Spring Data JPA repositories : `UserJpaRepository`, `ProjectJpaRepository`, `RoleJpaRepository`, `RefreshTokenJpaRepository`, `UserProjectRoleJpaRepository`
- [x] MapStruct mappers : `UserMapper`, `ProjectMapper`, `RoleMapper`, `RefreshTokenMapper`
- [x] Adapters : `UserRepositoryAdapter`, `ProjectRepositoryAdapter`, `RoleRepositoryAdapter`, `RefreshTokenRepositoryAdapter`, `UserProjectRoleRepositoryAdapter`

### Phase 5 — Sécurité ✅
- [x] `JwtService` : implémente `TokenPort`, génère/parse les tokens JWT
- [x] `JwtAuthenticationFilter` : lit le cookie `access_token`, charge le SecurityContext
- [x] `SecurityConfig` : CSRF disabled, stateless, routes publiques/privées/admin, CORS configurable
- [x] `BcryptPasswordHashAdapter` : implémente `PasswordHashPort`
- [x] `CookieUtils` : cookies HttpOnly, Secure, SameSite=Strict

### Phase 6 — Services + DTOs ✅
- [x] DTOs : `LoginRequest`, `CreateUserRequest`, `AssignRoleRequest`, `UpdateUserRequest`, `AuthResponse`, `ProjectResponse`, `UserResponse`, `ApiErrorResponse`
- [x] `AuthService` : login, refresh (token rotation), logout
- [x] `UserService` : CRUD utilisateurs, assignation rôles (SuperAdmin uniquement)
- [x] `ProjectService` : projets publics, projets accessibles, tous les projets (SuperAdmin)

### Phase 7 — Controllers ✅
- [x] `AuthController` : POST /api/auth/login, /refresh, /logout
- [x] `UserController` : GET /api/users/me, /api/users/me/projects
- [x] `ProjectController` : GET /api/projects/public
- [x] `AdminController` : /api/admin/users (CRUD), /api/admin/projects
- [x] `GlobalExceptionHandler` : réponses JSON normalisées

### Phase 8 — Frontend React ✅
- [x] Vite + React 18 + TypeScript + React Router
- [x] `AuthContext` + `useAuth` hook
- [x] Services : `api.ts` (refresh auto sur 401), `authService.ts`, `projectService.ts`
- [x] Pages : `HomePage`, `LoginPage`, `DashboardPage`, `UnauthorizedPage`
- [x] Composants : `Header`, `PrivateRoute`
- [x] Design system CSS cohérent avec la page statique
- [x] PWA : `manifest.json` + `service-worker.js`

### Phase 9 — Dockerisation ✅
- [x] `backend/Dockerfile` : multi-stage (JDK 21 build + JRE 21 runtime)
- [x] `frontend/Dockerfile` : multi-stage (Node build + Nginx Alpine)
- [x] `frontend/nginx.conf` : proxy /api/ → backend, SPA routing, headers sécurité
- [x] `docker-compose.prod.yml` : stack complète production

### Phase 10 — CI/CD ✅
- [x] `.github/workflows/ci.yml` : build + test backend (avec PostgreSQL), build frontend
- [x] `.github/workflows/deploy.yml` : build & push GHCR, déploiement SSH VPS

---

## À faire pour aller au-delà du MVP

- [ ] Page admin frontend (gestion utilisateurs/projets via interface)
- [ ] Réinitialisation de mot de passe (table `password_reset_tokens` prête, endpoint manquant)
- [ ] Email transactionnel (JavaMailSender ou service externe)
- [ ] PWA offline avancé (sync background, IndexedDB)
- [ ] Tests unitaires et d'intégration complets
- [ ] Configuration Nginx SSL sur le VPS (Cloudflare Full Strict)

```
