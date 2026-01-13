<div align="center">
    <picture>
        <source media="(prefers-color-scheme: dark)" srcset="composeApp/src/commonMain/composeResources/drawable/claco2_slogan_bg_png.png">
        <source media="(prefers-color-scheme: light)" srcset="composeApp/src/wasmJsMain/resources/claco2_slogan_svg.svg">
        <img alt="ClacO2 logo" src="composeApp/src/wasmJsMain/resources/claco2_slogan_svg.svg" width="300px">
    </picture>
</div>

<br>

# ClacOxygen

<br>
<div align="center">
    <a href="https://github.com/RISE-Remote-Intranet-School-Environment/ClacOxygen/releases">
        <img src="https://img.shields.io/github/v/release/RISE-Remote-Intranet-School-Environment/ClacOxygen?style=for-the-badge&color=D32F2F&labelColor=1a1a1a">
    </a>
    <a href="https://kotlinlang.org/">
        <img src="https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF.svg?style=for-the-badge&labelColor=1a1a1a&logo=kotlin&logoColor=7F52FF">
    </a>
    <a href="https://github.com/RISE-Remote-Intranet-School-Environment/ClacOxygen/blob/main/LICENSE">
        <img src="https://img.shields.io/static/v1.svg?style=for-the-badge&label=License&message=MIT&colorA=1a1a1a&colorB=D32F2F"/>
    </a>
</div>
<br>

> **ClacO2** centralise l'expérience étudiante ECAM dans une application unique.

Le client Kotlin Compose Multiplatform (Android, iOS, Desktop, Web) s'appuie sur un backend Ktor et une base SQLite pour synchroniser l'horaire, les cours, les ressources et le suivi académique. Le projet vise un usage quotidien : accès rapide aux informations clés, parcours clair, données unifiées et mêmes fonctionnalités sur chaque plateforme.

**Fonctionnalités :**

- Affichage des formations, blocs et cours
- Consultation des fiches ECTS, ressources et professeurs
- Calendrier global et horaire de cours
- Gestion des cours personnels (PAE)
- Authentification JWT et OAuth Microsoft

---

## Table des matières

- [Objectif](#objectif)
- [Histoire du logo](#histoire-du-logo)
- [Vue d'ensemble](#vue-densemble)
- [Fonctionnement du site](#fonctionnement-du-site)
- [Parcours utilisateur](#parcours-utilisateur)
- [Données, API et base](#données-api-et-base)
- [Architecture et flux](#architecture-et-flux)
- [Prérequis](#prérequis)
- [Installation locale](#installation-locale)
- [Lancer le serveur](#lancer-le-serveur)
- [Lancer l'app Desktop](#lancer-lapp-desktop)
- [Lancer l'app Web](#lancer-lapp-web)
- [Lancer l'app Android](#lancer-lapp-android)
- [Lancer l'app iOS](#lancer-lapp-ios)
- [Identifiants de test](#identifiants-de-test)
- [Organisation du depot](#organisation-du-depot)
- [Versioning](#versioning)
- [Dépannage](#dépannage)
- [Bugs et corrections](#bugs-et-corrections)
- [Recommendations futures](#recommendations-futures)
- [Reprendre le projet](#reprendre-le-projet)
- [Auteurs](#auteurs)

---

## Objectif

Centraliser l’expérience étudiant autour d’un client multiplateforme et d’un serveur unique, avec des données académiques structurées, un calendrier exploitable et un parcours simple pour gérer ses cours et ses ressources.

---

## Histoire du logo

<div align="center">
    <picture>
        <source media="(prefers-color-scheme: dark)" srcset="composeApp/src/commonMain/composeResources/drawable/claco2_slogan_bg_png.png">
        <source media="(prefers-color-scheme: light)" srcset="composeApp/src/wasmJsMain/resources/claco2_slogan_svg.svg">
        <img alt="ClacO2 logo" src="composeApp/src/wasmJsMain/resources/claco2_slogan_svg.svg" width="250px">
    </picture>
</div>

<br>

Pour nous éloigner un peu de notre très subtile allusion à une plateforme concurrente, nous avons ajouté deux thématiques : l'`Oxygène` et le `Clac`. Symbolisés respectivement par le `rouge` pour l'`O₂` et par l'assemblage de deux pièces de `puzzle` pour le son `Clac`.

Le slogan vient encore souligner ces deux concepts avec `Breathe` qui incite l'utilisateur à se relaxer et à accepter la bouffée d'oxygène qu'est notre application. Ainsi que l'onomatopée jumelle à `Clac`, `Clic`, qui évoque l'immédiateté de notre solution.

---

## Vue d'ensemble

Le projet est découpé en deux blocs :

| Module | Description |
|--------|-------------|
| [composeApp/](composeApp/) | Client Compose Multiplatform (Android, iOS, Desktop, Web) |
| [server/](server/) | Serveur Ktor + API REST + Base SQLite |

Le Web est servi par le serveur après génération du bundle WebAssembly. Le même backend alimente toutes les plateformes.

**Liens utiles :**
- [Voir la documentation du Client (App)](composeApp/README.md)
- [Voir la documentation du Backend (Server)](server/README.md)

---

## Fonctionnement du site

Le site correspond à l’application Web. Il est hébergé et servi par le serveur Ktor.

**Adresse de reference :** [https://clacoxygen.msrl.be/](https://clacoxygen.msrl.be/)

**Etapes cote site :**
1. Génération du bundle WebAssembly
2. Démarrage du serveur
3. Accès via `http://localhost:28088` en local

---

## Parcours utilisateur

1. Connexion par email/mot de passe ou OAuth Microsoft
2. Accueil avec tableau de bord, cours suivis, recherche dans le catalogue
3. Navigation formations → blocs → cours pour consulter les fiches
4. Accès aux ressources de cours et aux supports associés
5. Consultation du calendrier global et de l'horaire de cours
6. Lecture du PAE, des notes et de l'état de validation
7. Recherche dans l'annuaire des professeurs
8. Paramètres avec mode daltonien et informations de session

---

## Données, API et base

L'API est disponible sous `/api` et alimente une base SQLite.

**Sources de donnees :**
- Seeds serveur dans [server/data](server/data)
- Ressources client dans [composeApp/src/commonMain/composeResources/files](composeApp/src/commonMain/composeResources/files)

**Exemples d'API :**

| Categorie | Endpoints |
|-----------|-----------|
| Auth | `/api/auth/login`, `/api/auth/register`, `/api/auth/microsoft/login` |
| Formations | `/api/formations/with-courses`, `/api/blocs` |
| Cours | `/api/courses`, `/api/courses/{id}/details` |
| Horaires | `/api/course-schedule`, `/api/course-schedule/my-schedule` |
| Calendrier | `/api/calendar` |
| Professeurs | `/api/professors` |
| PAE | `/api/pae-students`, `/api/notes-students/by-student/{studentId}` |

**Base de donnees :** [server/data/app.db](server/data/app.db)

---

## Architecture et flux

```
┌─────────────────┐     ┌─────────────┐     ┌──────────┐
│ Client Compose  │────>│  API Ktor   │────>│  SQLite  │
│ (Android/iOS/   │<────│  (Backend)  │<────│   (DB)   │
│  Desktop/Web)   │     └─────────────┘     └──────────┘
└─────────────────┘
```

**Flux principaux :**
- **Authentification** : formulaire → `/api/auth/login` → JWT → appels protégés
- **Catalogue** : `/api/formations/with-courses` → affichage formations/blocs/cours
- **Cours étudiant** : `/api/my-courses` → ajout, suppression, affichage
- **Horaire** : `/api/course-schedule` + filtres année/série → vue calendrier

---

## Prérequis

- JDK 17+
- Android Studio (pour Android)
- Xcode (pour iOS)
- Gradle via le wrapper du dépôt

> [!IMPORTANT]
> Ce projet nécessite **JDK 17** ou supérieur. Assurez-vous que votre variable `JAVA_HOME` pointe vers une installation compatible avant de lancer les commandes Gradle.

---

## Installation locale

```sh
git clone https://github.com/RISE-Remote-Intranet-School-Environment/ClacOxygen.git
cd ClacOxygen
```

Verifier que JDK 17 est bien sélectionné.

---

## Lancer le serveur

Le serveur écoute sur le port `28088` (variable `PORT`).

Windows :

```shell
.\gradlew.bat :server:run
```

macOS/Linux :

```shell
./gradlew :server:run
```

## Lancer l’app Desktop

Windows :

```shell
.\gradlew.bat :composeApp:run
```

macOS/Linux :

```sh
./gradlew :composeApp:run
```

---

## Lancer l'app Web

1) Générer le bundle Wasm

Windows :

```shell
.\gradlew.bat :composeApp:wasmJsBrowserDistribution
```

macOS/Linux :

```shell
./gradlew :composeApp:wasmJsBrowserDistribution
```

2) Démarrer le serveur

Windows :

```shell
.\gradlew.bat :server:run
```

macOS/Linux :

```shell
./gradlew :server:run
```

3. Ouvrir `http://localhost:28088`

> [!TIP]
> Si la page Web reste blanche ou vide, c'est souvent parce que les ressources statiques n'ont pas été régénérées ou ne sont pas servies depuis le bon dossier. Relancez la tâche `wasmJsBrowserDistribution` pour corriger cela.

---

## Lancer l'app Android

1. Configurer local.properties avec le chemin SDK :
```properties
sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
```
2. Activer le mode développeur sur le téléphone.
3. Brancher le téléphone à l'ordinateur.
4. Utiliser l'USB pour le transfert de fichiers.
2. Assembler et installer :
```sh
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug
```

---

## Lancer l'app iOS

Ouvrir iosApp dans Xcode et lancer.

## Identifiants de test

| Email | Mot de passe |
|-------|--------------|
| `nschell@ecam.be` | `nicolas` |
| `ncrepin@ecam.be` | `nirina` |

---

## Organisation du depot

```
ClacOxygen/
├── composeApp/          # Client Kotlin Compose Multiplatform
├── iosApp/              # Point d'entrée iOS
├── server/              # Serveur Ktor, routes, services, base SQLite
├── shared/              # Code partagé
├── wasm-dist/           # Bundle WebAssembly (généré)
├── flake.nix            # Configuration NixOS
└── README.md            # Ce fichier
```

---

## Versioning

La version de l'application est centralisée dans le fichier [build.gradle.kts](build.gradle.kts) racine.
Un hook Gradle génère automatiquement la classe `BuildConfig` pour le client.

Pour créer une nouvelle release :
1. Incrémenter `appVersion` et `appVersionCode` dans [build.gradle.kts](build.gradle.kts).
2. Commit et tag sur master (ex: `v2.1.42`).
3. GitHub Actions construit et publie la release automatiquement.

---

## Dépannage

| Probleme | Solution |
|----------|----------|
| Port occupé | Definir `PORT` avant de lancer le serveur |
| Web vide | Relancer `:composeApp:wasmJsBrowserDistribution` puis `:server:run` |
| Auth échoué | Verifier que le serveur est demarre sur `http://localhost:28088` |
| Gradle bloque | Executer `./gradlew --stop` puis relancer |

---

## Bugs et corrections

- La backdoor admin hardcodée doit être supprimée pour éviter un accès non contrôlé.
- La validation des données dans les DTO doit être renforcée pour éviter des états incohérents.
- Les entrées PAE doivent être reliées aux cours ajoutés manuellement en base.
- La gestion des points des étudiants doit être corrigée et vérifiable.
- Les photos de professeurs doivent être mises en cache pour réduire les requêtes.

## Recommendations futures

- SQLite est parfait pour ce prototype mais limite la scalabilité horizontale. Une migration vers PostgreSQL est à envisager.
- Intégrer Redis pour le caching.
- Augmenter la couverture des tests.
- Mettre en place des pools de connexion (comme HikariCP).
- Remplacer les onglets folders par ceux de Claco (soit un lien vers claco directement soit un navigateur intégré dans l'app (demander à bypass la connexion à claco via l'OAuth)).
- Un scraping automatique doit s’exécuter à intervalle avec un indicateur d’heure de mise à jour dans l’UI, ou être remplacé par un accès à l’API calendar.
- Le local du professeur doit être lié au local du cours en temps réel.
- L’UX doit éviter d'avoir à la fois un menu à gauche ou une bottom bar selon le format retenu.
- Les relations entre tables SQL doivent être renforcées pour éviter les doublons et les incohérences.
- Utiliser plus d'attributs Microsoft (OAuth) pour préremplir les comptes en DB.
- Un monitoring doit signaler une hausse des requêtes en échec avec un système d’alerte.
- Un panel admin doit permettre la gestion des utilisateurs depuis l’app sans modification du code ni de la base.
- Un panel admin doit aussi permettre d’ajouter et modifier les professeurs.
- L’application doit proposer une séparation fonctionnelle par rôle entre admin, professor et student.
- Les étudiants et professeurs doivent pouvoir ajouter des événements personnalisés.
- Une option doit indiquer l’occupation des locaux.
- La version prof doit afficher l’horaire du professeur connecté.
- La version étudiant doit afficher l’horaire du professeur.
- Un accès direct avec recherche vers l’horaire des professeurs doit être disponible.
- Les photos de professeurs doivent être mises en cache.
- Les professeurs doivent pouvoir moduler leur page de cours et ajouter des ressources.
- Les professeurs doivent pouvoir publier des notifications liées à leurs cours.
- Des deadlines de projets doivent être liées au calendrier étudiant et professeur.
- Les liens entre cours, professeurs, horaires, locaux et ressources doivent être consolidés lors de l'ajout des méthode CRUD.
- Les données des étudiants (PAE, parcours) doivent être enrichies et consolidées.
- Le PAE doit afficher des indicateurs de progression par bloc.
- Uniformiser le PAE manuel et l'officiel.
- Les assets doivent être standardisés par plateforme avec des variantes d’icônes pour l’accessibilité.

## Reprendre le projet

Pour reprendre rapidement :

1. Démarrer le serveur et vérifier `http://localhost:28088`.
2. Lancer l’app Desktop pour valider l’UI sans émulateur.
3. Tester l’API avec les routes listées dans la section Données et API.
4. Lire [server/README.md](server/README.md) pour le détail des modèles et des routes.
5. Explorer [composeApp/src/commonMain](composeApp/src/commonMain) pour comprendre les écrans et la navigation.


## Auteurs

| Nom | Role |
|-----|------|
| Chokayri Omar | UI Desktop |
| Crépin Nirina | Scrum master + UI Android |
| Masureel Bruno | Backend |
| Schell Nicolas | UI iOS |
| Yaya Libis Issakha | Backend |
| Yildirim Arifcan | UI Web |
