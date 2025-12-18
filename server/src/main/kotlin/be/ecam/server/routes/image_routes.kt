package be.ecam.server.routes

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import java.awt.Image

fun Route.imageProxyRoutes() {
    // Client HTTP pour que le serveur télécharge les images
    val client = HttpClient(CIO) {
        followRedirects = true
    }

    route("/image-proxy") {
        head {
            val url = call.request.queryParameters["url"]
            if (url.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest)
                return@head
            }
            call.respond(HttpStatusCode.OK)
        }

        get {
            val url = call.request.queryParameters["url"]
            val widthParam = call.request.queryParameters["width"]?.toIntOrNull()
            
            if (url.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing 'url' parameter")
                return@get
            }

            try {
                val response = client.get(url)
                val bytes = response.readRawBytes()
                val originalContentType = response.contentType() ?: ContentType.Image.Any

                // Si pas de redimensionnement demandé ou image trop petite, on renvoie tel quel
                if (widthParam == null || bytes.size < 100_000) { // < 100KB
                    call.respondBytes(bytes, originalContentType)
                    return@get
                }

                // --- Logique de redimensionnement ---
                val inputStream = ByteArrayInputStream(bytes)
                val originalImage = ImageIO.read(inputStream)

                if (originalImage == null) {
                    // Si ImageIO ne peut pas lire (ex: format webp non supporté par défaut), on renvoie les bytes bruts
                    call.respondBytes(bytes, originalContentType)
                    return@get
                }

                // Calcul du ratio
                val targetWidth = widthParam
                val ratio = targetWidth.toDouble() / originalImage.width.toDouble()
                val targetHeight = (originalImage.height * ratio).toInt()

                // Création de l'image redimensionnée
                val resizedImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
                val graphics = resizedImage.createGraphics()
                graphics.drawImage(originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null)
                graphics.dispose()

                // Écriture dans un buffer
                val outputStream = ByteArrayOutputStream()
                // On convertit tout en JPEG pour la légèreté et compatibilité
                ImageIO.write(resizedImage, "jpg", outputStream)
                
                // CORRECTION : JPEG en majuscules
                call.respondBytes(outputStream.toByteArray(), ContentType.Image.JPEG)

            } catch (e: Exception) {
                // En cas d'erreur, on log et on renvoie une erreur serveur
                e.printStackTrace()
                call.respond(HttpStatusCode.BadGateway, "Failed to process image: ${e.message}")
            }
        }
    }
}