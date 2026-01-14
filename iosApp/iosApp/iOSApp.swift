import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Ce bloc se déclenche quand Safari renvoie vers l'app
                    print("🔗 URL REÇUE : \(url.absoluteString)")

                    // On vérifie que c'est bien notre URL de login
                    guard let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
                          url.scheme == "be.ecam.companion" else {
                        return
                    }

                    // Extraction des tokens
                    let accessToken = components.queryItems?.first(where: { $0.name == "accessToken" })?.value
                    let refreshToken = components.queryItems?.first(where: { $0.name == "refreshToken" })?.value

                    if let token = accessToken {
                        // On envoie les tokens au code Kotlin
                        AuthHelper.shared.handleCallback(accessToken: token, refreshToken: refreshToken)
                    }
                }
        }
    }
}