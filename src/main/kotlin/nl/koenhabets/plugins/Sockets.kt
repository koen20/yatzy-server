package nl.koenhabets.plugins

import io.ktor.serialization.kotlinx.*
import kotlinx.serialization.json.*
import io.ktor.server.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import nl.koenhabets.model.*
import nl.koenhabets.storage.StorageMysql
import java.util.*
import kotlin.collections.LinkedHashSet

fun Application.configureSockets(storage: StorageMysql) {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }

    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("ws") {
            val thisConnection = Connection(this)
            connections += thisConnection

            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val res = Json.decodeFromString<Message>(frame.readText())

                            if (res.action == Action.login) {
                                val actionRes = Json.decodeFromJsonElement<Message.Login>(res.data)
                                thisConnection.loggedIn = true
                                thisConnection.userId = actionRes.userId
                            } else if (thisConnection.loggedIn) {
                                if (res.action == Action.subscribe) {
                                    val actionRes = Json.decodeFromJsonElement<Message.Subscribe>(res.data)
                                    thisConnection.subscriptions.add(actionRes.userId)
                                } else if (res.action == Action.score) {
                                    val actionRes = Json.decodeFromJsonElement<Message.Score>(res.data)
                                    connections.forEach { connection ->
                                        connection.subscriptions.forEach {
                                            if (it == thisConnection.userId) {
                                                connection.session.send(actionRes.toString())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is Frame.Close -> {
                            println("closed")
                        }
                    }
                }
            } finally {
                println("Removing $thisConnection!")
                connections -= thisConnection
            }
        }
    }
}
