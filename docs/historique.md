# Application Caixadev

## Etat des lieux

J'ai un VPS sur OVH.
J'utilise Cloudflare pour la gestion du domaine caixadev.dev.
J'ai une page statique dans la racine de ce repo (index.html), actuellement hébergée sur GitHub Pages, mais je souhaite la migrer vers mon VPS.

## Objectifs de ce projet

Transformer cette page statique en une application web dynamique, avec un backend en Java et une base de données pour stocker les données des utilisateurs et les projets.
caixadev.dev = montre la page statique actuelle (cette page montre mes projets, et comment me contacter).
Chaque projet sera accessible depuis cette page. Certains projets seront uniquement accessibles après authentification.

Lorsque je crée un projet, je veux pouvoir choisir qui y a accès, créer des rôles pour les utilisateurs, créer des utilisateurs, et attribuer des rôles à ces utilisateurs pour chaque projet.
Si un projet a besoin d'un backend et d'une authentification, on utilsera le backend de caixadev.dev pour gérer l'authentification et les autorisations d'accès à ce projet. La base de données de caixadev.dev stockera les données des utilisateurs, des projets, des rôles et des autorisations d'accès.
Pour le frontend, je veux utiliser React. Toutes les pages reprendront les mêmes éléments de design que la page actuelle, pour garder une cohérence visuelle. Je veux que le frontend soit responsive, pour que les utilisateurs puissent accéder à leurs projets depuis n'importe quel appareil.
Tous les projets seront PWA, pour que les utilisateurs puissent les installer sur leur appareil et y accéder facilement. En hors ligne, les utilisateurs pourront accéder à leurs projets et leurs actions seront synchronisées avec le backend dès qu'ils seront de nouveau en ligne.


En tant que SuperAdmin, je veux pouvoir me connecter à l'application pour gérer tous les projets et les utilisateurs.
En tant que SuperAdmin, je veux avoir accès à tous les projets et toutes les bases de données, pour pouvoir les gérer efficacement.
En tant que SuperAdmin, je n'apparais dans aucun projet, ne suis jamais listé, mes actions ne sont jamais visibles par les autres utilisateurs, quelque soit leur rôle, quelque soit leur projet. Invisible TOUJOURS.
En tant que SuperAdmin, je veux pouvoir lire les messages des applications, absolument tous les messages même les messages éphémères des applications, pour pouvoir les modérer et les supprimer si nécessaire, de façon TOUJOURS INVISIBLE.
En tant que SuperAdmin, je veux pouvoir modifier la page caixadev.dev pour ajouter, supprimer ou modifier les projets affichés.


En tant que utilsateur non connecté, je veux pouvoir voir la page caixadev.dev et les projets qui y sont affichés, mais je ne peux pas accéder aux projets protégés par une authentification.
En tant que utilsateur non connecté, je veux pouvoir voir les projets qui sont accessibles sans authentification, pour les consulter et les utiliser s'ils sont gratuits, ou voir leur page de démonstration s'ils sont payants.

En tant que utilisateur connecté, je veux pouvoir accéder à mes projets protégés par une authentification, pour les consulter et les utilier s'ils me sont attribués, ou voir leur page de démonstration s'ils sont payants.
En tant que utilisateur connecté, je peux avoir plusieurs projets qui me sont attribués, et je veux pouvoir les consulter facilement depuis mon tableau de bord.
En tant que utilisateur connecté, je peux avoir plusieurs rôles différents dans chaque projet (par exemple, je peux être un contributeur dans un projet et un simple utilisateur dans un autre), et je veux que ces rôles soient clairement indiqués dans mon tableau de bord pour chaque projet.

Il n'y aura donc qu'une seule page login, qui dirigera les utilisateurs vers leurs projets après authentification, en fonction de leurs rôles et des autorisations d'accès qui leur sont attribués pour chaque projet. Si un utilisateur n'a accès à aucun projet, il verra un message lui indiquant qu'il n'a accès à aucun projet, et il pourra contacter l'admini du projet en question pour demander l'accès.
Si un utilisateur est inscrit à plusieurs projets, il verra tous les projets auxquels il a accès dans son tableau de bord, avec les rôles qui lui sont attribués pour chaque projet. Il pourra cliquer sur chaque projet pour y accéder, et il sera redirigé vers la page d'accueil de ce projet après authentification.


### Décisions prises

Une base de donées PostgreSQL pour stocker les données des utilisateurs, des projets, des rôles et des autorisations d'accès.
Un backend en Java avec Spring Boot pour gérer l'authentification, les autorisations d'accès et les interactions avec la base de données.
Un frontend en React pour créer une interface utilisateur dynamique et responsive.
Utilisation de Docker pour faciliter le déploiement de l'application sur le VPS.
Utilisation de Flyway pour gérer les migrations de la base de données.
Utilisation de JWT pour gérer l'authentification et les autorisations d'accès. Un token JWT, un refresh token, et un token de réinitialisation de mot de passe, HttpOnly, Secure, SameSite=Strict, avec une durée de vie de 15 minutes pour le token JWT, 7 jours pour le refresh token, et 1 heure pour le token de réinitialisation de mot de passe.
Utilisation de Cloudflare pour gérer le domaine et sécuriser l'accès à l'application.
Utilisation de Github pour héberger le code source et gérer les versions de l'application. Une branche main pour la production, une branche develop pour le développement, et des branches de fonctionnalités pour chaque nouvelle fonctionnalité.
Utilisation de GitHub Actions pour automatiser les tests et le déploiement de l'application sur le VPS.
Utilisation de PWA pour permettre aux utilisateurs d'installer l'application sur leur appareil et d'y accéder facilement, même en hors ligne.
Authentification de type RBAC + Scope
Orientation multi-tenant : chaque projet est un tenant, les utilisateurs peuvent avoir des rôles différents dans chaque projet, et les données de chaque projet sont isolées les unes des autres.
Donc : chaque requête envoyée au backend doit inclure le projet ciblé, pour que le backend puisse vérifier les autorisations d'accès de l'utilisateur pour ce projet et accéder aux données de ce projet dans la base de données.
Backend : architecture hexagonale, avec une séparation claire entre les différentes couches de l'application (contrôleurs, services, repositories, etc.) pour faciliter la maintenance et l'évolution de l'application.
Backend : utilisation de DTOs pour transférer les données entre les différentes couches de l'application, et éviter les fuites de données sensibles.
Backend : pas de librairie tierce non indispensable pour éviter les dépendances inutiles et garder une application légère. Utilisation des fonctionnalités natives de Java et de Spring Boot autant que possible, et éviter les librairies tierces pour des fonctionnalités qui peuvent être implémentées facilement avec les fonctionnalités natives.
On peut utiliser des librairies spring boot, comme Lombok, mapstruct, validator, etc... pour faciliter le développement et éviter de réinventer la roue, mais on évite les librairies qui ajoutent une complexité inutile ou qui ne sont pas indispensables pour le fonctionnement de l'application.
Frontend : architecture en composants, avec une séparation claire entre les différentes parties de l'interface utilisateur (composants de présentation, composants de conteneur, etc.) pour faciliter la maintenance et l'évolution de l'application.
Pas de librairie tierce (pas de tailwind, pas de redux, etc...) pour garder une application légère et éviter les dépendances inutiles.

