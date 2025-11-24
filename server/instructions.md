# Mise à jour Backend — Avancement (24/11/2025)

## 1. Ce qui est terminé

### 1.1 Catalogue ECAM (formations, blocs, cours)

- Seed complet depuis JSON
- Tables, modèles et services opérationnels
- Routes publiques fonctionnelles
- CRUD admin disponible
- Module testé et stable

### 1.2 Calendrier global ECAM

- Modèle et table créés
- Import complet depuis le JSON
- Routes GET fonctionnelles (par owner, par catégorie, par groupe)
- Données conformes à l’organisation ECAM

### 1.3 Calendrier des cours (course schedule)

- Table dédiée créée (cours, horaires, groupes)
- Seed automatique depuis le fichier JSON
- Routes GET fonctionnelles : par date, par groupe, par cours
- Données pleinement exploitables pour le frontend

### 1.4 Fiches ECAM (Course Details)

- Table relationnelle 1→1 implantée
- Import complet depuis `ecam_courses_details_2025.json`
- Routes disponibles :

```text
/api/courses/{id}/details
/api/courses/code/{code}/details
```

- Tests validés, données conformes à `plus.ecam.be`

### 1.5 Annuaire des professeurs

- Table, modèle et service complets
- Seed intégral depuis `ecam_professors_2025.json`
- Routes disponibles :

```text
/api/professors
/api/professors/{id}
/api/professors/email/{email}
/api/professors/speciality/{spec}
```

- CRUD complet opérationnel
- Import massif validé (100+ professeurs)

### 1.6 Mise à jour de `Application.kt`

- Tous les seeds automatiques activés (formations, cours, détails, horaires, événements, professeurs)
- Toutes les routes intégrées proprement sous `/api`
- Backend stable, base SQLite `app.db` reconstituée correctement

## 2. Prochaines étapes

### 2.1 Module Étudiants (Users)

- Création de la table `students`
- Champs suggérés : `ecam_email`, `year`, `program`
- Suivi des inscriptions aux cours
- Gestion des notes (PAE)
- Ressources personnelles (PDF, documents, images)

### 2.2 Module Professeurs (extensions)

- Association professeur ↔ cours enseignés
- Possibilité d'ajouter une photo (upload + stockage)
- Permissions étendues (accès aux notes, gestion des cours)

### 2.3 Ressources de cours

- Upload de fichiers
- Métadonnées (titre, description, type)
- Liaison ressources ↔ cours
- Organisation en sections / sous-sections

### 2.4 Jointures globales

- Renforcer les liens entre cours ↔ professeurs ↔ calendrier
- Ajouter `course_id` dans les événements du calendrier lorsque pertinent
- Fournir des vues/endpoint combinés (cours + prof + local + horaire)

### 2.5 Permissions et rôles (JWT)

- Ajouter rôles : `admin`, `professeur`, `étudiant`
- Définir les niveaux d’accès et permissions par rôle
- Protéger les routes sensibles

### 2.6 Jeu de données complet pour le frontend

- Déjà fourni : formations, cours, fiches ECT, professeurs, horaires, événements ECAM
- À ajouter : données étudiantes (PAE et parcours individuels)

---

