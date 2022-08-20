package nl.koenhabets.plugins

import io.ktor.serialization.kotlinx.*
import kotlinx.serialization.json.*
import io.ktor.server.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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
        webSocket("api/v1/ws") {
            val thisConnection = Connection(this)
            connections += thisConnection

            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val res = Json.decodeFromString<Message>(frame.readText())
                            if (res.action == ActionType.login) {
                                var success = false
                                val actionRes = Json.decodeFromJsonElement<Message.Login>(res.data)
                                if (actionRes.key !== null) { //todo check if key and userid is correct
                                    thisConnection.loggedIn = true
                                    thisConnection.userId = actionRes.userId
                                    success = true
                                }
                                val loginResponse = Response.LoginResponse(success)
                                val response = Response(ResponseType.loginResponse, Json.encodeToJsonElement(loginResponse).jsonObject)
                                thisConnection.session.send(Json.encodeToString(response))
                            } else if (thisConnection.loggedIn) {
                                if (res.action == ActionType.subscribe) {
                                    val actionRes = Json.decodeFromJsonElement<Message.Subscribe>(res.data)
                                    if (!thisConnection.subscriptions.contains(actionRes.userId)) {
                                        thisConnection.subscriptions.add(actionRes.userId)
                                    }
                                } else if (res.action == ActionType.score) {
                                    val actionRes = Json.decodeFromJsonElement<Message.Score>(res.data)
                                    connections.forEach { connection ->
                                        connection.subscriptions.forEach {
                                            if (it == thisConnection.userId) {
                                                val response = Response(ResponseType.scoreResponse, Json.encodeToJsonElement(actionRes).jsonObject)
                                                connection.session.send(Json.encodeToString(response))
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
