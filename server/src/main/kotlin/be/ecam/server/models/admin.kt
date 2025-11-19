// models/admin.kt : for model exposed + table admin

package be.ecam.server.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.IntIdTable

// === TABLE (schematic) ===
object AdminTable : IntIdTable("admins") {
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 100).uniqueIndex()
    val password = varchar("password", 255) // stocke HASH 
}

// === ENTITY (ORM) ===
// this class represents a row in the AdminTable (admin.new { ... }, Admin.find { ... })
class Admin(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Admin>(AdminTable)
    var username by AdminTable.username
    var email by AdminTable.email
    var password by AdminTable.password
}
