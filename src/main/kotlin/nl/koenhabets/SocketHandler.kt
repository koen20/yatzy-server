package nl.koenhabets

import io.ktor.websocket.*
import kotlinx.serialization.json.*
import nl.koenhabets.model.*
import nl.koenhabets.storage.StorageMysql

class SocketHandler(
    private val thisConnection: Connection,
    private val connections: MutableSet<Connection>,
    private val storage: StorageMysql,
    private val json: Json
) {
    suspend fun handleMessage(frame: Frame) {
        when (frame) {
            is Frame.Text -> {
                val res = json.decodeFromString<Message>(frame.readText())
                if (res.action == ActionType.login) {
                    handleLogin(res.data)
                } else if (thisConnection.loggedIn) {
                    when (res.action) {
                        ActionType.subscribe -> handleSubscribe(res.data)
                        ActionType.score -> handleScore(res.data)
                        ActionType.endGame -> handleEndGame(res.data)
                        ActionType.login -> handleLogin(res.data)
                    }
                }

            }
            is Frame.Close -> {
                println("closed")
            }
            else -> {}
        }
    }

    private suspend fun handleLogin(data: JsonObject) {
        var success = false
        val actionRes = json.decodeFromJsonElement<Message.Login>(data)
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
    }

    private suspend fun handleSubscribe(data: JsonObject) {
        val actionRes = json.decodeFromJsonElement<Message.Subscribe>(data)
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
    }

    suspend fun handleScore(data: JsonObject) {
        if (thisConnection.userId !== null) {
            val actionRes = json.decodeFromJsonElement<Message.Score>(data)
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
    }

    fun handleEndGame(data: JsonObject) {
        val actionRes = json.decodeFromJsonElement<Message.EndGame>(data)
        if (actionRes.game != "test" && thisConnection.highestScoreCount > 7) {
            storage.gameDao.addGame(actionRes)
        }
    }
}
