// models/adminDTOS.kt : for admin related DTOs with CRUD operations

package be.ecam.server.models
import kotlinx.serialization.Serializable

// === DATA TRANSFER OBJECT (DTO) ===
// here we define the data structures used for admin management routes

// DTO for admin information (/apoi/admins/...)
@Serializable 
data class AdminDTO(
    val id: Int, 
    val username: String, 
    val email: String)

// DTO for updating an existing admin 
@Serializable 
data class UpdateAdminRequest(
    val username: String? = null, 
    val email: String? = null, 
    val password: String? = null)  // if updating password, we will hash it in service layer


