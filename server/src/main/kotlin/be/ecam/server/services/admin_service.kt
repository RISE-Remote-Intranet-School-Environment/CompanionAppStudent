package be.ecam.server.services

import at.favre.lib.crypto.bcrypt.BCrypt
import be.ecam.server.models.Admin
import be.ecam.server.models.AdminDTO
import be.ecam.server.models.AdminTable
import be.ecam.server.models.UpdateAdminRequest
import org.jetbrains.exposed.sql.transactions.transaction

object AdminService {

    // READ ALL 
    fun getAllAdmins(): List<AdminDTO> = transaction {
        Admin.all().map {
            AdminDTO(
                id = it.id.value,
                username = it.username,
                email = it.email
            )
        }
    }

    // READ ONE 
    fun getAdminById(id: Int): AdminDTO? = transaction {
        val a = Admin.findById(id) ?: return@transaction null
        AdminDTO(
            id = a.id.value,
            username = a.username,
            email = a.email
        )
    }

    // UPDATE 
    fun updateAdmin(id: Int, req: UpdateAdminRequest): AdminDTO? = transaction {
        val a = Admin.findById(id) ?: return@transaction null

        req.username?.let { a.username = it.trim() }
        req.email?.let { a.email = it.trim() }
        req.password?.let {
            val hashed = BCrypt
                .withDefaults()
                .hashToString(12, it.toCharArray())
            a.password = hashed
        }

        AdminDTO(
            id = a.id.value,
            username = a.username,
            email = a.email
        )
    }

    // DELETE 
    fun deleteAdmin(id: Int): Boolean = transaction {
        val a = Admin.findById(id) ?: return@transaction false
        a.delete()
        true
    }
}
