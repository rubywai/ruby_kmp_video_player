import SwiftUI
import RubyExampleComposeApp

struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        RubyExampleViewControllerKt.RubyExampleViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
