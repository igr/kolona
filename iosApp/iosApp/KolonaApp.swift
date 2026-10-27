import SwiftUI
import KolonaShared

@main
struct KolonaApp: App {
    @Environment(\.scenePhase) private var scenePhase
    private let game = IosGame()

    var body: some Scene {
        WindowGroup {
            GameView(game: game)
                .ignoresSafeArea()
                .preferredColorScheme(.dark)
                .onChange(of: scenePhase) { phase in
                    if phase != .active { game.pause() }
                }
        }
    }
}

private struct GameView: UIViewControllerRepresentable {
    let game: IosGame
    func makeUIViewController(context: Context) -> UIViewController { game.viewController() }
    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}
