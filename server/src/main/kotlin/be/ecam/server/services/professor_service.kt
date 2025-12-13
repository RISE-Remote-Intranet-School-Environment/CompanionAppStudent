package be.ecam.server.services

import be.ecam.server.models.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object ProfessorService {

    // json professors format

    @Serializable
    private data class ProfessorsRootJson(
        val year: String,
        val generated_at: String,
        val source: String,
        val professors: List<ProfessorJson>
    )

    @Serializable
    private data class ProfessorJson(
        val id: Int,
        val first_name: String,
        val last_name: String,
        val email: String,
        val speciality: String,
        val office: String? = null,
        val phone: String? = null
    )

    fun seedProfessorsFromJson() {
        val resource = ProfessorService::class.java.classLoader
            .getResource("files/ecam_professors_2025.json")
            ?: error("Resource 'files/ecam_professors_2025.json' not found in classpath")

        val text = resource.readText()
        val json = Json { ignoreUnknownKeys = true }

        val root = json.decodeFromString<ProfessorsRootJson>(text)
        val list = root.professors

        transaction {
            list.forEach { p ->

                val existing = Professor.find { ProfessorsTable.email eq p.email }
                    .firstOrNull()

                if (existing == null) {
                    // INSERT
                    Professor.new {
                        firstName = p.first_name
                        lastName = p.last_name
                        email = p.email
                        speciality = p.speciality
                        office = p.office
                        phone = p.phone
                    }
                } else {
                    // UPDATE
                    existing.apply {
                        firstName = p.first_name
                        lastName = p.last_name
                        speciality = p.speciality
                        office = p.office
                        phone = p.phone
                    }
                }
            }
        }
    }

    
    // read professors
   
    fun getAllProfessors(): List<ProfessorDTO> = transaction {
        Professor.all().map { it.toDto() }
    }

    fun getProfessorById(id: Int): ProfessorDTO? = transaction {
        Professor.findById(id)?.toDto()
    }

    fun getProfessorByEmail(email: String): ProfessorDTO? = transaction {
        Professor.find { ProfessorsTable.email eq email }
            .firstOrNull()
            ?.toDto()
    }

    fun getProfessorsBySpeciality(spec: String): List<ProfessorDTO> = transaction {
        Professor.find { ProfessorsTable.speciality eq spec }
            .map { it.toDto() }
    }

   // crearte professor

    fun createProfessor(req: ProfessorWriteRequest): ProfessorDTO = transaction {
        val existing = Professor.find { ProfessorsTable.email eq req.email }.firstOrNull()
        if (existing != null) error("Email déjà utilisé")

        val p = Professor.new {
            firstName = req.firstName
            lastName = req.lastName
            email = req.email
            office = req.office
            phone = req.phone
            speciality = req.speciality
        }

        p.toDto()
    }

    // update professor

    fun updateProfessor(id: Int, req: ProfessorWriteRequest): ProfessorDTO? = transaction {
        val p = Professor.findById(id) ?: return@transaction null

        p.firstName = req.firstName
        p.lastName = req.lastName

        p.office = req.office
        p.phone = req.phone
        p.speciality = req.speciality

        p.toDto()
    }

    // delete professor

    fun deleteProfessor(id: Int): Boolean = transaction {
        val p = Professor.findById(id) ?: return@transaction false
        p.delete()
        true
    }

    // mapper professor 

    private fun Professor.toDto(): ProfessorDTO =
        ProfessorDTO(
            id = id.value,
            firstName = firstName,
            lastName = lastName,
            email = email,
            office = office,
            phone = phone,
            speciality = speciality
        )
}
