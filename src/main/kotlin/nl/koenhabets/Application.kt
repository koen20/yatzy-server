package nl.koenhabets

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import nl.koenhabets.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureSecurity()
        configureSockets()
    }.start(wait = true)
}
