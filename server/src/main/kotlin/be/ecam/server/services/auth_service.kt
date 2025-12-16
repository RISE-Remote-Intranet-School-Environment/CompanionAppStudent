package be.ecam.server.services

import at.favre.lib.crypto.bcrypt.BCrypt
import be.ecam.server.models.*
import be.ecam.server.security.JwtConfig
import be.ecam.server.security.JwtService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object AuthService {

    // Helper pour générer la réponse avec tokens et stockage DB
    private fun generateAuthResponse(user: AuthUserDTO, message: String): AuthResponse {
        val accessToken = JwtService.generateAccessToken(user)
        val refreshToken = JwtService.generateRefreshToken(user)

        // Stockage du Refresh Token en DB
        RefreshTokensTable.insert {
            it[userId] = user.id
            it[token] = refreshToken
            it[expiresAt] = System.currentTimeMillis() + JwtConfig.REFRESH_TOKEN_EXPIRATION
        }

        return AuthResponse(user, message, accessToken, refreshToken)
    }

    // register
    fun register(req: RegisterRequest): AuthResponse = transaction {
        val username = req.username.trim()
        val email = req.email.trim()

        require(username.isNotBlank()) { "Username vide" }
        require(email.isNotBlank()) { "Email vide" }
        require(req.password.isNotBlank()) { "Mot de passe vide" }

        val exists = UsersTable
            .selectAll()
            .where { (UsersTable.username eq username) or (UsersTable.email eq email) }
            .singleOrNull()

        require(exists == null) { "Utilisateur déjà existant" }

        val passwordHash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
        val role = UserRole.STUDENT

        val id = UsersTable.insertAndGetId {
            it[UsersTable.username] = username
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[firstName] = ""
            it[lastName] = ""
            it[UsersTable.role] = role
        }.value

        val user = AuthUserDTO(id, username, email, role, null)
        generateAuthResponse(user, "Compte créé")
    }

    // login
    fun login(req: LoginRequest): AuthResponse = transaction {
        val identifier = req.emailOrUsername.trim()
        val row = UsersTable
            .selectAll()
            .where { (UsersTable.username eq identifier) or (UsersTable.email eq identifier) }
            .singleOrNull()
            ?: error("Utilisateur introuvable")

        val validPassword = BCrypt.verifyer()
            .verify(req.password.toCharArray(), row[UsersTable.passwordHash])
            .verified

        require(validPassword) { "Mot de passe incorrect" }

        val user = AuthUserDTO(
            id = row[UsersTable.id].value,
            username = row[UsersTable.username],
            email = row[UsersTable.email],
            role = row[UsersTable.role],
            avatarUrl = row[UsersTable.avatarUrl]
        )

        generateAuthResponse(user, "Connexion OK")
    }

    // Refresh Token Flow
    fun refreshToken(oldRefreshToken: String): AuthResponse? = transaction {
        // 1. Chercher le token en DB
        val tokenRow = RefreshTokensTable
            .selectAll()
            .where { RefreshTokensTable.token eq oldRefreshToken }
            .singleOrNull()
            ?: return@transaction null

        // 2. Vérifier expiration
        if (tokenRow[RefreshTokensTable.expiresAt] < System.currentTimeMillis()) {
            RefreshTokensTable.deleteWhere { token eq oldRefreshToken }
            return@transaction null
        }

        // 3. Récupérer l'user
        val userIdVal = tokenRow[RefreshTokensTable.userId]
        val user = getUserById(userIdVal) // Réutilise la méthode existante

        // 4. Rotation : Supprimer l'ancien, créer le nouveau
        RefreshTokensTable.deleteWhere { token eq oldRefreshToken }
        
        // On génère une nouvelle réponse complète (nouveau access + nouveau refresh)
        generateAuthResponse(user, "Token rafraîchi")
    }

    // get/auth/me
    fun getUserById(userId: Int): AuthUserDTO = transaction {
        val row = UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()
            ?: error("User not found")
        
        AuthUserDTO(
            id = row[UsersTable.id].value,
            username = row[UsersTable.username],
            email = row[UsersTable.email],
            role = row[UsersTable.role],
            avatarUrl = row[UsersTable.avatarUrl]
        )
    }
    
    // put/auth/me
    fun updateMe(userId: Int, newUsername: String, newEmail: String): AuthUserDTO = transaction {

        val username = newUsername.trim()
        val email = newEmail.trim()

        require(username.isNotBlank()) { "Username vide" }
        require(email.isNotBlank()) { "Email vide" }

        val current = UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?: error("User not found")

        val conflict = UsersTable
            .selectAll()
            .where {
                ((UsersTable.username eq username) or (UsersTable.email eq email)) and
                (UsersTable.id neq userId)
            }
            .singleOrNull()

        require(conflict == null) { "Username ou email déjà utilisé" }

        UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.username] = username
            it[UsersTable.email] = email
        }

        AuthUserDTO(
            id = userId,
            username = username,
            email = email,
            role = current[UsersTable.role],
            avatarUrl = current[UsersTable.avatarUrl]
        )
    }
}