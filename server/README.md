# ClacOxygen - Documentation Serveur

Ce dossier contient le serveur backend Ktor, qui expose l'API REST, gère la logique métier et maintient la base de données SQLite.

**Liens utiles :**
- [Retour à la documentation générale](../README.md)
- [Voir la documentation du Client (App)](../composeApp/README.md)

## Sommaire

1. [Objectif du module](#1-objectif-du-module)
2. [Architecture du serveur](#2-architecture-du-serveur)
3. [Organisation du code](#3-organisation-du-code)
4. [Base de données et Modélisation](#4-base-de-donnees-et-modelisation)
5. [API et DTOs](#5-api-et-dtos)
6. [Sécurité et Authentification](#6-securite-et-authentification)
7. [Configuration et Démarrage](#7-configuration-et-demarrage)
8. [Déploiement NixOS (Optionnel)](#8-deploiement-nixos-optionnel)
9. [Principes de conception (SOLID)](#9-principes-de-conception-solid)
10. [Diagrammes et limites](#10-diagrammes-et-limites)

## 1. Objectif du module

Le backend joue le rôle de source unique de vérité pour l'application. Le frontend ne communique jamais directement avec la base de données.

Les responsabilités du backend incluent :
- L'authentification et l'autorisation des utilisateurs (Email/Password + OAuth Microsoft).
- La validation des données entrantes.
- L'application des règles métier.
- L'accès et la transformation des données.
- La sécurisation des routes via JWT.
- L'exposition de réponses JSON stables via des DTO.

## 2. Architecture du serveur

Le backend repose sur une architecture en couches (layered architecture), séparant nettement les responsabilités.

- **Routes** : Définissent les endpoints HTTP, gèrent les requêtes/réponses et appellent les services. Elles ne contiennent aucune logique métier complexe.
- **Services** : Cœur fonctionnel. Ils contiennent la logique métier, appliquent les règles et orchestrent l'accès aux données.
- **Accès aux données (DAO/Table)** : Interaction avec la base SQLite via Exposed.
- **DTO (Data Transfer Objects)** : Modèles exposés au frontend via l'API, découplés de la structure interne de la base.

### Structure des sources

```
server/
|-- src/
|   |-- main/
|   |   |-- resources/
|   |   |   |-- application.conf      # Configuration Ktor
|   |   |   |-- logback.xml           # Configuration des logs
|   |   |   |-- static/               # Fichiers statiques (HTML callbacks)
|   |   |-- kotlin/be/ecam/server/
|   |   |   |-- Application.kt        # Point d'entrée
|   |   |   |-- config/               # Configuration typée
|   |   |   |-- db/
|   |   |   |   |-- DatabaseFactory.kt # Connexion SQLite
|   |   |   |-- models/
|   |   |   |   |-- *_models.kt       # Tables Exposed et entités internes
|   |   |   |-- routes/
|   |   |   |   |-- *_routes.kt       # Endpoints API (Ktor Routing)
|   |   |   |-- services/
|   |   |   |   |-- *_service.kt      # Logique métier
|   |   |   |-- security/
|   |   |   |   |-- PasswordHasher.kt # BCrypt
|   |   |   |   |-- TokenService.kt   # Gestion JWT
```

## 3. Organisation du code

Le code est organisé de manière modulaire. Pour chaque entité principale (ex: Cours, Professeur, Auth) :

1.  Un fichier **model** définit la table SQL.
2.  Un **service** contient la logique métier.
3.  Une **route** expose l'API correspondante.

Cette organisation facilite la navigation et limite le couplage.

## 4. Base de données et Modélisation

La base de données utilisée est **SQLite**.
Ce choix a été motivé par le contexte académique : aucune configuration serveur requise, portabilité via un fichier unique (`app.db`), et rapidité de mise en place.

### Modélisation

La modélisation distingue :
- **Identifiants techniques** : `id` auto-incrémentés.
- **Identifiants métier** : codes uniques (ex: `Q2`, `4MIN`, `C1234`).

### JetBrains Exposed (ORM)

L'accès aux données utilise le framework Exposed. Il permet d'écrire des requêtes type-safe en Kotlin. Les résultats (`ResultRow`) sont immédiatement transformés en objets métier ou DTO pour ne pas exposer la structure SQL.

## 5. API et DTOs

La communication s'effectue exclusivement en JSON via **Kotlinx Serialization**.

Les **DTO (Data Transfer Objects)** définissent le contrat d'interface avec le client. Ils permettent :
- De masquer les champs sensibles (hash de mot de passe, sels).
- De stabiliser l'API même si le schéma de base de données change.
- D'adapter le format des données aux besoins de l'UI (ex: dates formatées, listes imbriquées).

## 6. Sécurité et Authentification

La sécurité repose sur **JWT (JSON Web Token)**.

Flux d'authentification :
1. L'utilisateur s'authentifie (Login standard ou OAuth).
2. Le backend vérifie l'identité.
3. Un `AccessToken` (courte durée) et un `RefreshToken` (longue durée) sont générés.
4. Le client doit inclure le token dans le header `Authorization: Bearer <token>` pour les routes protégées.

Les mots de passe locaux sont hachés via **BCrypt**.

## 7. Configuration et Démarrage

Le fichier `Application.kt` est le point d'entrée. Il configure :
- Les plugins Ktor (ContentNegotiation, CORS, Auth, CallLogging).
- La connexion DB via `DatabaseFactory`.
- L'enregistrement des routes.

Le serveur écoute par défaut sur le port défini par la variable d'environnement `PORT` (défaut: 28088).

## 8. Déploiement NixOS (Optionnel)

Le projet propose une intégration avec l'écosystème Nix pour garantir la reproductibilité du build et faciliter le déploiement. Bien que le serveur puisse être déployé via un JAR standard ou un conteneur Docker classique, l'approche NixOS est privilégiée pour l'infrastructure de production.

### Packaging

Le serveur est packagé via `gradle2nix` qui verrouille toutes les dépendances Gradle. Le `flake.nix` à la racine du projet expose le package `default` (serveur) et un package `full` (serveur + client web statique).

### Module NixOS

Un module NixOS est exporté par le Flake pour configurer le service, le reverse proxy Nginx, et la gestion des secrets via `sops-nix` automatiquement.

### Exemple de configuration

Voici comment importer et configurer le module dans un système NixOS :

```nix
{
  config,
  lib,
  pkgs,
  inputs,
  ...
}:

{
  imports = [
    # Import du module depuis le flake inputs
    inputs.clacoxygen.nixosModules.default
  ];

  # Gestion des secrets (Tokens, Clés API) via sops
  sops.secrets.clacoxygen_jwt_secret = {
    owner = "clacoxygen";
    mode = "0400";
  };
  sops.secrets.clacoxygen_ms_client_id = {
    owner = "clacoxygen";
    mode = "0400";
  };
  sops.secrets.clacoxygen_ms_client_secret = {
    owner = "clacoxygen";
    mode = "0400";
  };

  # Configuration du service
  services.clacoxygen = {
    enable = true;
    port = 28088;
    domain = "clacoxygen.msrl.be";
    user = "clacoxygen";
    group = "clacoxygen";

    # Chemins vers les secrets (obligatoire en prod)
    jwtSecretFile = config.sops.secrets.clacoxygen_jwt_secret.path;
    msClientIdFile = config.sops.secrets.clacoxygen_ms_client_id.path;
    msClientSecretFile = config.sops.secrets.clacoxygen_ms_client_secret.path;

    # Configuration automatique du Reverse Proxy et SSL
    nginx = {
      enable = true;
      enableACME = true; # Certificats Let's Encrypt automatiques
    };
  };
}
```

L'import dans le flake.nix de l'infrastructure ressemblerait à ceci :

```nix
inputs.clacoxygen.url = "github:RISE-Remote-Intranet-School-Environment/CompanionAppStudent/";
```

## 9. Principes de conception (SOLID)

Le backend respecte les principes SOLID pour assurer la maintenabilité :

- **SRP** : Séparation stricte Routes / Services / Modèles.
- **OCP** : L'ajout de nouvelles fonctionnalités (ex: nouvelles routes) ne nécessite pas de modifier le noyau existant.
- **DIP** : Les services dépendent d'abstractions (interfaces implicites par injection) plutôt que de détails d'implémentation.

## 10. Diagrammes et limites

### Diagrammes
Des diagrammes de base de données (Logique et Physique) sont disponibles dans le dossier `data/` à la racine du module serveur. Ils illustrent les relations entre Formations, Blocs, Cours, Professeurs et Étudiants.

### Limites actuelles et évolutions
- **Base de données** : SQLite est parfait pour ce prototype mais limite la scalabilité horizontale. Une migration vers PostgreSQL est l'évolution naturelle.
- **Cache** : Absence actuelle de cache (Redis).
- **Tests** : La couverture de tests backend se concentre sur les tests d'intégration des routes critiques.
