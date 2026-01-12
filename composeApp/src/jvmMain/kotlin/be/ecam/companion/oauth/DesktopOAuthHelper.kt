package be.ecam.companion.oauth

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream

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
    
    fun start(): Int {
        stop()
        
        val port = findAvailablePort()
        
        server = HttpServer.create(InetSocketAddress("localhost", port), 0)
        callbackFuture = CompletableFuture()

        server?.createContext("/logo.svg") { exchange ->
            val logoBytes = javaClass.getResourceAsStream("/claco2_slogan_svg.svg")?.readBytes()
                ?: "".toByteArray()
            exchange.responseHeaders.add("Content-Type", "image/svg+xml")
            exchange.sendResponseHeaders(200, logoBytes.size.toLong())
            exchange.responseBody.use { it.write(logoBytes) }
        }
        
        server?.createContext("/callback") { exchange ->
            val query = exchange.requestURI.query ?: ""
            val params = parseQuery(query)
            
            val accessToken = params["accessToken"]
            val refreshToken = params["refreshToken"]
            val error = params["error"]
            
            val title = if (accessToken != null) "Connexion réussie" else "Erreur"
            val icon = if (accessToken != null) "✓" else "✕"
            val iconClass = if (accessToken != null) "success" else "error"
            val message = if (accessToken != null) 
                "Connexion réussie !" 
            else 
                "Erreur de connexion"
            
            val details = if (accessToken != null) 
                "Vous pouvez fermer cette fenêtre et retourner à l'application." 
            else 
                (error ?: "Erreur inconnue")

            val html = """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>$title - ClacO₂</title>
                    <style>
                        :root {
                            --ecam-blue: #003366;
                            --bg-gradient: linear-gradient(135deg, #002B55 0%, #004E92 100%);
                        }
                        body {
                            font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                            background: var(--bg-gradient);
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                            color: #333;
                        }
                        .card {
                            background: white;
                            padding: 40px;
                            border-radius: 16px;
                            box-shadow: 0 20px 40px rgba(0,0,0,0.2);
                            text-align: center;
                            max-width: 400px;
                            width: 90%;
                            animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1);
                        }
                        @keyframes slideUp {
                            from { transform: translateY(30px); opacity: 0; }
                            to { transform: translateY(0); opacity: 1; }
                        }
                        
                        .logo-image {
                            display: block;
                            margin: 0 auto 24px auto;
                            height: 80px;
                            width: 100%;
                            max-width: 280px;
                        }

                        h2 {
                            color: var(--ecam-blue);
                            margin: 0 0 12px 0;
                            font-size: 24px;
                        }
                        p {
                            color: #666;
                            line-height: 1.6;
                            margin: 0 0 24px 0;
                        }
                        .status-icon {
                            font-size: 48px;
                            margin-bottom: 16px;
                            display: block;
                        }
                        .success { color: #28a745; }
                        .error { color: #dc3545; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <img class="logo-image" src="/logo.svg" alt="ClacO₂">

                        <span class="status-icon $iconClass">$icon</span>
                        <h2 class="${if(accessToken == null) "error" else ""}">$message</h2>
                        <p>$details</p>
                        ${if (accessToken != null) "<script>setTimeout(function() { window.close(); }, 2500);</script>" else ""}
                    </div>
                </body>
                </html>
            """.trimIndent()
            
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            val responseBytes = html.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, responseBytes.size.toLong())
            exchange.responseBody.write(responseBytes)
            exchange.responseBody.close()
            
            // Compléter le future avec le résultat
            callbackFuture?.complete(OAuthResult(accessToken, refreshToken, error))
            
            // Arrêter le serveur après un court délai
            Thread {
                Thread.sleep(3000)
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
     * Timeout de 5 minutes.
     */
    fun waitForCallback(): OAuthResult? {
        return try {
            callbackFuture?.get(5, TimeUnit.MINUTES)
        } catch (e: Exception) {
            println("OAuth callback timeout or error: ${e.message}")
            OAuthResult(null, null, e.message)
        }
    }
    
    fun stop() {
        try {
            server?.stop(0)
        } catch (e: Exception) {
            // Ignorer les erreurs lors de l'arrêt
        }
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
        if (query.isBlank()) return emptyMap()
        
        return query.split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    try {
                        java.net.URLDecoder.decode(parts[0], "UTF-8") to 
                        java.net.URLDecoder.decode(parts[1], "UTF-8")
                    } catch (e: Exception) {
                        parts[0] to parts[1]
                    }
                } else null
            }
            .toMap()
    }
}
