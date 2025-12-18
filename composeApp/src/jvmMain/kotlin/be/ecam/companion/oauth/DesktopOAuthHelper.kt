package be.ecam.companion.oauth

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture

/**
 * Helper pour OAuth sur Desktop.
 * Démarre un serveur HTTP local temporaire pour recevoir le callback OAuth.
 */
object DesktopOAuthHelper {
    
    private var server: HttpServer? = null
    private var callbackFuture: CompletableFuture<OAuthResult>? = null
    
    data class OAuthResult(
        val accessToken: String?,
        val refreshToken: String?,
        val error: String?
    )
    
    /**
     * Démarre le serveur local et retourne le port utilisé.
     * Appeler startAndWait() pour attendre le callback.
     */
    fun start(): Int {
        // Trouver un port disponible
        val port = findAvailablePort()
        
        server = HttpServer.create(InetSocketAddress("localhost", port), 0)
        callbackFuture = CompletableFuture()
        
        server?.createContext("/callback") { exchange ->
            val query = exchange.requestURI.query ?: ""
            val params = parseQuery(query)
            
            val accessToken = params["accessToken"]
            val refreshToken = params["refreshToken"]
            val error = params["error"]
            
            // Répondre avec une page HTML qui confirme la réception
            val response = if (accessToken != null) {
                """
                <!DOCTYPE html>
                <html>
                <head><title>Connexion réussie</title></head>
                <body style="font-family: sans-serif; text-align: center; padding: 50px; background: linear-gradient(135deg, #1a237e, #0d47a1); color: white;">
                    <h1>✅ Connexion réussie !</h1>
                    <p>Vous pouvez fermer cette fenêtre et retourner à l'application.</p>
                </body>
                </html>
                """.trimIndent()
            } else {
                """
                <!DOCTYPE html>
                <html>
                <head><title>Erreur</title></head>
                <body style="font-family: sans-serif; text-align: center; padding: 50px; background: #ff5252; color: white;">
                    <h1>❌ Erreur</h1>
                    <p>${error ?: "Erreur inconnue"}</p>
                </body>
                </html>
                """.trimIndent()
            }
            
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.write(response.toByteArray())
            exchange.responseBody.close()
            
            // Compléter le future avec le résultat
            callbackFuture?.complete(OAuthResult(accessToken, refreshToken, error))
            
            // Arrêter le serveur après un court délai
            Thread {
                Thread.sleep(1000)
                stop()
            }.start()
        }
        
        server?.executor = null
        server?.start()
        
        println("OAuth local server started on port $port")
        return port
    }
    
    /**
     * Attend le callback OAuth et retourne le résultat.
     */
    fun waitForCallback(): OAuthResult? {
        return try {
            callbackFuture?.get()
        } catch (e: Exception) {
            OAuthResult(null, null, e.message)
        }
    }
    
    fun stop() {
        server?.stop(0)
        server = null
        callbackFuture = null
    }
    
    private fun findAvailablePort(): Int {
        // Essayer des ports dans une plage
        for (port in 18080..18100) {
            try {
                val socket = java.net.ServerSocket(port)
                socket.close()
                return port
            } catch (e: Exception) {
                // Port occupé, essayer le suivant
            }
        }
        throw RuntimeException("Aucun port disponible trouvé")
    }
    
    private fun parseQuery(query: String): Map<String, String> {
        return query.split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    java.net.URLDecoder.decode(parts[0], "UTF-8") to 
                    java.net.URLDecoder.decode(parts[1], "UTF-8")
                } else null
            }
            .toMap()
    }
}
