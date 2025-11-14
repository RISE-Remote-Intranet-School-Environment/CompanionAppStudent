package be.ecam.common

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
