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

        val user = AuthUserDTO(id, username, email, role, null, "", "")
        generateAuthResponse(user, "Compte créé")
    }

    /**
     * Login ou création automatique via Microsoft OAuth
     */
    fun loginOrRegisterMicrosoft(
        email: String,
        firstName: String,
        lastName: String,
        displayName: String? = null,
        avatarUrl: String? = null
    ): AuthResponse = transaction {
        
        require(email.isNotBlank()) { "Email Microsoft vide" }

        // 1. Chercher si l'utilisateur existe déjà
        val existingUser = UsersTable
            .selectAll()
            .where { UsersTable.email eq email.lowercase() }
            .singleOrNull()

        val userDto = if (existingUser != null) {
            // Utilisateur existant -> update les infos si nécessaire (photo, nom...)
            val userId = existingUser[UsersTable.id].value
            
            // Mise à jour de l'avatar si fourni et différent
            if (!avatarUrl.isNullOrBlank() && existingUser[UsersTable.avatarUrl] != avatarUrl) {
                UsersTable.update({ UsersTable.id eq userId }) {
                    it[UsersTable.avatarUrl] = avatarUrl
                }
            }
            
            // Mise à jour du prénom/nom si vides
            if (existingUser[UsersTable.firstName].isNullOrBlank() && firstName.isNotBlank()) {
                UsersTable.update({ UsersTable.id eq userId }) {
                    it[UsersTable.firstName] = firstName
                    it[UsersTable.lastName] = lastName
                }
            }
            
            AuthUserDTO(
                id = userId,
                username = existingUser[UsersTable.username],
                email = existingUser[UsersTable.email],
                role = existingUser[UsersTable.role],
                avatarUrl = avatarUrl ?: existingUser[UsersTable.avatarUrl],
                firstName = if (existingUser[UsersTable.firstName].isNullOrBlank()) firstName else existingUser[UsersTable.firstName],
                lastName = if (existingUser[UsersTable.lastName].isNullOrBlank()) lastName else existingUser[UsersTable.lastName]
            )
        } else {
            // Nouvel utilisateur -> création automatique
            val username = email.substringBefore("@").lowercase()
                .replace(".", "_")
                .take(50)
            
            val finalUsername = generateUniqueUsername(username)

            val newId = UsersTable.insertAndGetId {
                it[UsersTable.username] = finalUsername
                it[UsersTable.email] = email.lowercase()
                it[UsersTable.passwordHash] = ""
                it[UsersTable.firstName] = firstName
                it[UsersTable.lastName] = lastName
                it[UsersTable.role] = UserRole.STUDENT
                it[UsersTable.avatarUrl] = avatarUrl
            }.value

            AuthUserDTO(newId, finalUsername, email.lowercase(), UserRole.STUDENT, avatarUrl, firstName, lastName)
        }

        generateAuthResponse(userDto, "Connexion Microsoft OK")
    }

    /**
     * Génère un username unique en ajoutant un suffixe si nécessaire
     */
    private fun generateUniqueUsername(baseUsername: String): String {
        var candidate = baseUsername
        var counter = 1
        
        while (UsersTable.selectAll().where { UsersTable.username eq candidate }.count() > 0) {
            candidate = "${baseUsername}_$counter"
            counter++
        }
        
        return candidate
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
            avatarUrl = row[UsersTable.avatarUrl],
            firstName = row[UsersTable.firstName],
            lastName = row[UsersTable.lastName]
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
            avatarUrl = row[UsersTable.avatarUrl],
            firstName = row[UsersTable.firstName],
            lastName = row[UsersTable.lastName]
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
            avatarUrl = current[UsersTable.avatarUrl],
            firstName = current[UsersTable.firstName],
            lastName = current[UsersTable.lastName]
        )
    }
}