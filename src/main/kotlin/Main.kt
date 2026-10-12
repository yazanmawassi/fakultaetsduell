import gui.SopraApplication

/**
 * Main entry point that starts the [SopraApplication]
 *
 * Once the application is closed, it prints a message indicating the end of the application.
 */
fun main() {
    SopraApplication().show()
    println("Application ended. Goodbye")
}