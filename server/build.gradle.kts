plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    application
}

val appVersion: String by rootProject.extra

group = "be.ecam.server"
version = appVersion

application {
    mainClass.set("be.ecam.server.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)

    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.network.tls.certificates)
    implementation("io.ktor:ktor-client-cio:2.3.6") 
    implementation("io.ktor:ktor-server-cors:2.3.6")
    implementation("io.ktor:ktor-client-core:2.3.6")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.6")
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    // BCrypt for password hashing (hacheure de mot de passe)
    implementation("at.favre.lib:bcrypt:0.10.2")


    // SQLite Database
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")

    // Ktor Call Logging
    implementation("io.ktor:ktor-server-call-logging:2.3.4")

    // jwt for authentication
    implementation("io.ktor:ktor-server-auth:2.3.6")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.6")
    implementation("com.auth0:java-jwt:4.4.0")

    implementation("io.ktor:ktor-server-auto-head-response:2.3.6")

    implementation("io.ktor:ktor-server-rate-limit:2.3.6")
}
