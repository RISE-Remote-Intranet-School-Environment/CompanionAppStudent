package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CourseService {

    //  GET all courses
    fun getAllCourses(): List<CourseDTO> = transaction {
        CoursesTable
            .selectAll()
            .map { it.toCourseDTO() }
    }

    //  GET by DB id
    fun getCourseById(id: Int): CourseDTO? = transaction {
        CoursesTable
            .selectAll()
            .where { CoursesTable.id eq id }
            .singleOrNull()
            ?.toCourseDTO()
    }

    //  GET by logical courseId 
    fun getCoursesByCourseId(courseId: String): List<CourseDTO> = transaction {
        CoursesTable
            .selectAll()
            .where { CoursesTable.courseId eq courseId }
            .map { it.toCourseDTO() }
    }

    //  GET by raccourci id 
    fun getCoursesByShortId(shortId: String): List<CourseDTO> = transaction {
        CoursesTable
            .selectAll()
            .where { CoursesTable.courseRaccourciId eq shortId }
            .map { it.toCourseDTO() }
    }

    //  GET by blocId
    fun getCoursesByBlocId(blocId: String): List<CourseDTO> = transaction {
        CoursesTable
            .selectAll()
            .where { CoursesTable.blocId eq blocId }
            .map { it.toCourseDTO() }
    }

    //  GET by formationId
    fun getCoursesByFormationId(formationId: String): List<CourseDTO> = transaction {
        CoursesTable
            .selectAll()
            .where { CoursesTable.formationId eq formationId }
            .map { it.toCourseDTO() }
    }

    //  CREATE
    fun createCourse(req: CourseWriteRequest): CourseDTO = transaction {
        val newId = CoursesTable.insertAndGetId { row ->
            row[CoursesTable.courseId] = req.courseId
            row[CoursesTable.courseRaccourciId] = req.courseRaccourciId
            row[CoursesTable.title] = req.title
            row[CoursesTable.credits] = req.credits
            row[CoursesTable.periods] = req.periods
            row[CoursesTable.detailsUrl] = req.detailsUrl
            row[CoursesTable.mandatory] = req.mandatory
            row[CoursesTable.blocId] = req.blocId
            row[CoursesTable.formationId] = req.formationId
            row[CoursesTable.language] = req.language
            row[CoursesTable.icon] = req.icon ?: guessCourseIcon(req.title)
        }

        CoursesTable
            .selectAll()
            .where { CoursesTable.id eq newId }
            .single()
            .toCourseDTO()
    }

    //  UPDATE
    fun updateCourse(id: Int, req: CourseWriteRequest): CourseDTO? = transaction {
        val updated = CoursesTable.update({ CoursesTable.id eq id }) { row ->
            row[CoursesTable.courseId] = req.courseId
            row[CoursesTable.courseRaccourciId] = req.courseRaccourciId
            row[CoursesTable.title] = req.title
            row[CoursesTable.credits] = req.credits
            row[CoursesTable.periods] = req.periods
            row[CoursesTable.detailsUrl] = req.detailsUrl
            row[CoursesTable.mandatory] = req.mandatory
            row[CoursesTable.blocId] = req.blocId
            row[CoursesTable.formationId] = req.formationId
            row[CoursesTable.language] = req.language
            row[CoursesTable.icon] = req.icon ?: guessCourseIcon(req.title)
        }

        if (updated == 0) return@transaction null

        CoursesTable
            .selectAll()
            .where { CoursesTable.id eq id }
            .singleOrNull()
            ?.toCourseDTO()
    }

    //  DELETE
    fun deleteCourse(id: Int): Boolean = transaction {
        CoursesTable.deleteWhere { CoursesTable.id eq id } > 0
    }
}

fun guessCourseIcon(title: String): String {
    val t = title.lowercase()
    return when {
        listOf("chim", "phys", "science", "biolog", "lab", "optique", "spectro", "energie", "énergie").any { t.contains(it) } -> "Science"
        listOf("math", "alg", "anal", "stat", "prob", "calcul").any { t.contains(it) } -> "Functions"
        listOf("info", "prog", "code", "software", "algo", "informatique", "java", "python", "programmation", "matlab", "simulink", "operating system", "os ", "intelligence artificielle", "artificial intelligence", "ai", "gpu", "web", "logiciel", "développement", "development").any { t.contains(it) } -> "Code"
        listOf("anglais", "english", "langue", "communication", "fran", "presentation", "presse").any { t.contains(it) } -> "Language"
        listOf("network", "réseau", "reseau", "protocol", "internet", "telecom", "télécom", "tic").any { t.contains(it) } -> "Share"
        listOf("security", "secur", "sécur", "crypt", "cyber").any { t.contains(it) } -> "Lock"
        listOf("elec", "élec", "electric", "electron", "circuit", "power", "signal", "commande", "hvac", "capteur", "capteurs", "sustainable").any { t.contains(it) } -> "Bolt"
        listOf("meca", "méca", "mechan", "therm", "fluid", "structure", "resistance", "résistance", "materiaux", "matériaux", "cao", "dao", "dessin", "3d", "usinage", "fabrication", "béton", "beton", "armé", "charpente", "fondation", "sols", "hydraulique", "génie civil", "genie civil", "charpentes", "bois", "metal", "métall", "topographie", "stabilit", "geotech", "géotech", "geodes", "géodési", "urbanisme").any { t.contains(it) } -> "Build"
        listOf("project", "projet", "design", "stage", "atelier", "tp", "tfe", "seminaire", "séminaire", "travail de fin", "memoire", "expertise").any { t.contains(it) } -> "Assignment"
        listOf("gestion", "management", "finance", "eco", "éco", "organ", "droit", "entreprise", "marketing", "qualite", "qualité", "logistique", "comptabil", "entrepreneuriat", "insertion professionnelle", "regulatory", "strategie", "gouvernance", "exigence").any { t.contains(it) } -> "Work"
        listOf("data", "base", "sql", "bd", "database", "big data", "analyse de donn", "analyse des donn").any { t.contains(it) } -> "Storage"
        listOf("mobile", "android", "ios", "smart").any { t.contains(it) } -> "PhoneIphone"
        listOf("robot", "automation", "automatisation", "automatique", "mcu", "microcontroleur", "microcontrôleur", "instrumentation", "regulation", "régulation", "control", "controle", "installations", "process", "industrie 4.0", "mesure", "measurement", "modelling", "simulation", "digital", "embedded", "mechatron", "mécatron", "cnc", "system on chip", "soc", "maintenance", "technologie", "bureau d'etudes", "bureau d'études", "systémique").any { t.contains(it) } -> "Engineering"
        listOf("safety", "santé", "sante", "secours", "medical", "medic", "health", "pathologie").any { t.contains(it) } -> "HealthAndSafety"
        listOf("sport", "gym", "education physique", "physique sport").any { t.contains(it) } -> "SportsEsports"
        listOf("leadership", "éthique", "ethique", "culture", "humain", "human", "psych", "social").any { t.contains(it) } -> "Psychology"
        listOf("innovation", "creatif", "créatif").any { t.contains(it) } -> "EmojiObjects"
        else -> "MenuBook"
    }
}
