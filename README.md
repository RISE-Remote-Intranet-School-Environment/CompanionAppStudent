# ClacOxygen

ClacOxygen (ClacO₂) centralise l'expérience étudiante ECAM dans une application unique. Le client Kotlin Compose Multiplatform (Android, iOS, Desktop, Web) s'appuie sur un backend Ktor et une base SQLite pour synchroniser l'horaire, les cours, les ressources et le suivi académique. Le projet vise un usage quotidien : accès rapide aux informations clés, parcours clair, données unifiées et mêmes fonctionnalités sur chaque plateforme.

## Sommaire

1. [Objectif](#1-objectif)
2. [Histoire du logo](#2-histoire-du-logo)
3. [Vue d’ensemble](#3-vue-densemble)
4. [Fonctionnement du site](#4-fonctionnement-du-site)
5. [Parcours utilisateur](#5-parcours-utilisateur)
6. [Données, API et base](#6-donnees-api-et-base)
7. [Architecture et flux](#7-architecture-et-flux)
8. [Prérequis](#8-prerequis)
9. [Installation locale](#9-installation-locale)
10. [Lancer le serveur](#10-lancer-le-serveur)
11. [Lancer l’app Desktop](#11-lancer-lapp-desktop)
12. [Lancer l’app Web](#12-lancer-lapp-web)
13. [Lancer l’app Android](#13-lancer-lapp-android)
14. [Lancer l’app iOS](#14-lancer-lapp-ios)
15. [Identifiants de test](#15-identifiants-de-test)
16. [Organisation du dépôt](#16-organisation-du-depot)
17. [Dépannage](#17-depannage)
18. [Bugs et corrections](#18-bugs-et-corrections)
19. [Évolutions futures](#19-evolutions-futures)
20. [Reprendre le projet](#20-reprendre-le-projet)
21. [Auteurs et rôles](#21-auteurs-et-roles)

## 1. Objectif

Centraliser l’expérience étudiant autour d’un client multiplateforme et d’un serveur unique, avec des données académiques structurées, un calendrier exploitable et un parcours simple pour gérer ses cours et ses ressources.

## 2. Histoire du logo

<img src="composeApp/src/wasmJsMain/resources/claco2_slogan_svg.svg" alt="Logo" width="280">

Pour nous éloigner un peu de notre très subtile allusion à une plateforme concurrente, nous avons ajouté deux thématiques : l'`Oxygène` et le `Clac`. Symbolisés respectivement par le `rouge` pour l'`O₂` et par l'assemblage de deux pièces de `puzzle` pour le son `Clac`.

Le slogan vient encore souligner ces deux concepts avec `Breathe` qui incite l'utilisateur à se relaxer et à accepter la bouffée d'oxygène qu'est notre application. Ainsi que l'onomatopée jumelle à `Clac`, `Clic`, qui évoque l'immédiateté de notre solution.

## 3. Vue d’ensemble

Le projet est découpé en deux blocs :

- un client Compose Multiplatform (Android, iOS, Desktop, Web),
- un serveur Ktor qui expose l’API et alimente une base SQLite.

Le Web est servi par le serveur après génération du bundle WebAssembly. Le même backend alimente toutes les plateformes.

## 4. Fonctionnement du site

Le site correspond à l’application Web. Il est hébergé et servi par le serveur Ktor.

Adresse de référence : `https://clacoxygen.msrl.be/`

Étapes côté site :

- génération du bundle WebAssembly,
- démarrage du serveur,
- accès via `http://localhost:28088` en local.

## 5. Parcours utilisateur

Le parcours est pensé pour un étudiant qui veut accéder vite à ses informations :

1. Connexion par email/mot de passe ou OAuth Microsoft.
2. Accueil avec tableau de bord, cours suivis, recherche dans le catalogue et gestion des cours.
3. Navigation formations → blocs → cours pour consulter les fiches.
4. Accès aux ressources de cours et aux supports associés.
5. Consultation du calendrier global et de l’horaire de cours, avec filtres par année et série.
6. Lecture du PAE, des notes et de l’état de validation.
7. Recherche dans l’annuaire des professeurs et accès aux détails.
8. Paramètres avec mode daltonien et informations de session.

## 6. Données, API et base

L’API est disponible sous `/api` et alimente une base SQLite.

Sources de données :

- Seeds serveur dans `server/data` (formations, cours, horaires, professeurs, événements).
- Ressources client dans `composeApp/src/commonMain/composeResources/files`.

Exemples d’API utilisées par le client :

- Auth : `/api/auth/login`, `/api/auth/register`, `/api/auth/microsoft/login`, `/api/auth/me`
- Formations : `/api/formations/with-courses`, `/api/blocs`
- Cours : `/api/courses`, `/api/courses/{id}/details`
- Horaires : `/api/course-schedule`, `/api/course-schedule/my-schedule`
- Calendrier : `/api/calendar`
- Professeurs : `/api/professors`
- Cours utilisateur : `/api/my-courses`
- PAE : `/api/pae-students`, `/api/notes-students/by-student/{studentId}`
- Ressources : `/api/courses/{code}/resources`, `/api/course-resources`

Base de données : `server/data/app.db`.

L'explication complète du backend se trouve dans [server/README.md](server/README.md).
La documentation du UI client se trouve dans [composeApp/README.md](composeApp/README.md).

## 7. Architecture et flux

Schéma logique :

- Client (Compose) → API Ktor → SQLite
- Client Web (Wasm) → API Ktor → SQLite

Flux principaux :

- Authentification : formulaire → `/api/auth/login` → JWT → appels protégés.
- Catalogue : `/api/formations/with-courses` → affichage formations/blocs/cours.
- Cours étudiant : `/api/my-courses` → ajout, suppression, affichage.
- Ressources : `/api/courses/{code}/resources` → liste filtrée par cours.
- Horaire : `/api/course-schedule` + filtres année/série → vue calendrier.
- PAE et notes : `/api/pae-students` + `/api/notes-students/by-student/{id}`.

## 8. Prérequis

- JDK 17
- Android Studio pour Android
- Xcode pour iOS
- Gradle via le wrapper du dépôt

## 9. Installation locale

1) Cloner le dépôt.
2) Ouvrir le projet dans l’IDE.
3) Vérifier que JDK 17 est bien sélectionné.

## 10. Lancer le serveur

Le serveur écoute sur le port `28088` (variable `PORT`).

Windows :

```shell
.\gradlew.bat :server:run
```

macOS/Linux :

```shell
./gradlew :server:run
```

## 11. Lancer l’app Desktop

Windows :

```shell
.\gradlew.bat :composeApp:run
```

macOS/Linux :

```shell
./gradlew :composeApp:run
```

## 12. Lancer l’app Web

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

Ouvrir `http://localhost:28088`.

## 13. Lancer l'app Android

### Configuration Android (local.properties)

Pour compiler et installer l'app sur un appareil Android, le fichier `local.properties` doit exister à la racine du projet et contenir le chemin vers le SDK Android.

Exemple (Windows) :

```
sdk.dir=C:\\Users\\<votre_user>\\AppData\\Local\\Android\\Sdk
```

Pour assembler l'application avant l'installation
---

```shell
.\gradlew.bat :composeApp:assembleDebug --no-daemon --no-configuration-cache
```

Puis lancer la commande d'installation avec le téléphone relié avec un câble usb(-c)
---

```shell
.\gradlew.bat :composeApp:installDebug
```
**Note**: Pour que l'installation fonctionne sur le téléphone, installez le mode développeur dessus. Puis, accepter le FTP du lien USB.
## 14. Lancer l’app iOS

Ouvrir `iosApp/` dans Xcode et lancer.

## 15. Identifiants de test

- `nschell@ecam.be` / `nicolas`
- `ncrepin@ecam.be` / `nirina`

## 16. Organisation du dépôt

- `composeApp/` : client Kotlin Compose Multiplatform.
- `iosApp/` : point d’entrée iOS.
- `server/` : serveur Ktor, routes, services, base SQLite.
- `shared/` : code partagé.

## 17. Dépannage

- Port occupé : définir `PORT` avant de lancer le serveur.
- Web vide : relancer `:composeApp:wasmJsBrowserDistribution`, puis `:server:run`.
- Auth échoue : vérifier que le serveur est démarré et que le client pointe vers `http://localhost:28088`.
- Gradle bloqué : exécuter `.\gradlew.bat --stop` (Windows) ou `./gradlew --stop` (macOS/Linux), puis relancer la tâche.

## 18. Bugs et corrections

- La backdoor admin hardcodée doit être supprimée pour éviter un accès non contrôlé.
- Le token JWT ne doit plus être affiché dans les paramètres de l’application.
- La validation des données dans les DTO doit être renforcée pour éviter des états incohérents.
- Les entrées PAE doivent être reliées aux cours ajoutés manuellement en base.
- La gestion des points des étudiants doit être corrigée et vérifiable.
- Les photos de professeurs doivent être mises en cache pour réduire les requêtes.

## 19. Recommendations futures

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

## 20. Reprendre le projet

Pour reprendre rapidement :

1. Démarrer le serveur et vérifier `http://localhost:28088`.
2. Lancer l’app Desktop pour valider l’UI sans émulateur.
3. Tester l’API avec les routes listées dans la section Données et API.
4. Lire `server/README.md` pour le détail des modèles et des routes.
5. Explorer `composeApp/src/commonMain` pour comprendre les écrans et la navigation.

## 21. Auteurs et rôles

- Chokayri Omar : UI Desktop
- Crépin Nirina : Scrum master + UI Android
- Masureel Bruno : Backend
- Schell Nicolas : UI iOS
- Yaya Libis Issakha : Backend
- Yildirim Arifcan : UI Web
