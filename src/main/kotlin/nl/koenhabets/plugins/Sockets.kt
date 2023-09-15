package nl.koenhabets.plugins

import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import nl.koenhabets.StatsCollector
import nl.koenhabets.model.*
import nl.koenhabets.storage.StorageMysql
import java.time.Duration
import java.util.*

fun Application.configureSockets(storage: StorageMysql, statsCollector: StatsCollector) {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
    }

    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        val json = Json {
            ignoreUnknownKeys = true
        }
        webSocket("api/v1/ws") {
            val thisConnection = Connection(this)
            connections += thisConnection
            statsCollector.setWsConnected(connections.size)

            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val res = json.decodeFromString<Message>(frame.readText())
                            if (res.action == ActionType.login) {
                                var success = false
                                val actionRes = json.decodeFromJsonElement<Message.Login>(res.data)
                                if (storage.userDao.checkUser(actionRes.userId, actionRes.key)) {
                                    thisConnection.loggedIn = true
                                    thisConnection.userId = actionRes.userId
                                    success = true
                                }
                                val loginResponse = Response.LoginResponse(success)
                                val response = Response(
                                    ResponseType.loginResponse,
                                    json.encodeToJsonElement(loginResponse).jsonObject
                                )
                                thisConnection.session.send(json.encodeToString(response))
                            } else if (thisConnection.loggedIn) {
                                if (res.action == ActionType.subscribe) {
                                    val actionRes = json.decodeFromJsonElement<Message.Subscribe>(res.data)
                                    if (!thisConnection.subscriptions.contains(actionRes.userId)) {
                                        thisConnection.subscriptions.add(actionRes.userId)
                                        connections.forEach {
                                            if (it.userId == actionRes.userId && it.lastScoreResponse != null) {
                                                val response = Response(
                                                    ResponseType.scoreResponse,
                                                    json.encodeToJsonElement(it.lastScoreResponse).jsonObject
                                                )
                                                thisConnection.session.send(json.encodeToString(response))
                                                actionRes.pairCode?.let { pairCode ->
                                                    thisConnection.userId?.let {userId ->
                                                        val pairResponse = Response.PairResponse(userId, pairCode)
                                                        val pResponse = Response(
                                                            ResponseType.pairResponse,
                                                            json.encodeToJsonElement(pairResponse).jsonObject
                                                        )
                                                        it.session.send(json.encodeToString(pResponse))
                                                    }

                                                }

                                                return@forEach
                                            }

                                        }
                                    }
                                } else if (res.action == ActionType.score) {
                                    if (thisConnection.userId !== null) {
                                        val actionRes = json.decodeFromJsonElement<Message.Score>(res.data)
                                        thisConnection.updateHighestScoreCount(actionRes.fullScore)
                                        thisConnection.lastScoreResponse = Response.ScoreResponse(
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
                                                        json.encodeToJsonElement(thisConnection.lastScoreResponse).jsonObject
                                                    )
                                                    connection.session.send(json.encodeToString(response))
                                                }
                                            }
                                        }
                                    }
                                } else if (res.action == ActionType.endGame) {
                                    val actionRes = json.decodeFromJsonElement<Message.EndGame>(res.data)
                                    if (actionRes.game != "test" && thisConnection.highestScoreCount > 7) {
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
                statsCollector.setWsConnected(connections.size)
            }
        }
    }
}
