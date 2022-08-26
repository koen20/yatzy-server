package nl.koenhabets.plugins

import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import nl.koenhabets.model.*
import nl.koenhabets.storage.StorageMysql
import java.time.Duration
import java.util.*

fun Application.configureSockets(storage: StorageMysql) {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
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
                                if (storage.userDao.checkUser(actionRes.userId, actionRes.key)) {
                                    thisConnection.loggedIn = true
                                    thisConnection.userId = actionRes.userId
                                    success = true
                                }
                                val loginResponse = Response.LoginResponse(success)
                                val response = Response(
                                    ResponseType.loginResponse,
                                    Json.encodeToJsonElement(loginResponse).jsonObject
                                )
                                thisConnection.session.send(Json.encodeToString(response))
                            } else if (thisConnection.loggedIn) {
                                if (res.action == ActionType.subscribe) {
                                    val actionRes = Json.decodeFromJsonElement<Message.Subscribe>(res.data)
                                    if (!thisConnection.subscriptions.contains(actionRes.userId)) {
                                        thisConnection.subscriptions.add(actionRes.userId)
                                    }
                                } else if (res.action == ActionType.score) {
                                    if (thisConnection.loggedIn && thisConnection.userId !== null) {
                                        val actionRes = Json.decodeFromJsonElement<Message.Score>(res.data)
                                        val scoreResponse = Response.ScoreResponse(
                                            actionRes.username,
                                            thisConnection.userId!!,
                                            actionRes.game,
                                            actionRes.score,
                                            actionRes.fullScore,
                                            actionRes.lastUpdate
                                        )
                                        connections.forEach { connection ->
                                            connection.subscriptions.forEach {
                                                if (it == thisConnection.userId) {
                                                    val response = Response(
                                                        ResponseType.scoreResponse,
                                                        Json.encodeToJsonElement(scoreResponse).jsonObject
                                                    )
                                                    connection.session.send(Json.encodeToString(response))
                                                }
                                            }
                                        }
                                    }
                                } else if (res.action == ActionType.endGame) {
                                    val actionRes = Json.decodeFromJsonElement<Message.EndGame>(res.data)
                                    if (actionRes.game !== "test") {
                                        storage.gameDao.addGame(actionRes)
                                    }
                                }
                            }
                        }

                        is Frame.Close -> {
                            println("closed")
                        }

                        else -> {}
                    }
                }
            } finally {
                connections -= thisConnection
            }
        }
    }
}
