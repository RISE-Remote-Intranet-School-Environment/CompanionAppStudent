package be.ecam.server.routes

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.imageProxyRoutes() {
    // Client HTTP pour que le serveur télécharge les images
    val client = HttpClient()

    route("/image-proxy") {
        get {
            val url = call.request.queryParameters["url"]
            if (url.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing 'url' parameter")
                return@get
            }

            try {
                val response = client.get(url)
                val contentType = response.contentType() ?: ContentType.Image.Any
                
                call.respondBytes(response.readBytes(), contentType)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadGateway, "Failed to fetch image: ${e.message}")
            }
        }
    }
}