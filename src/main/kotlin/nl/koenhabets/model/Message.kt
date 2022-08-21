package nl.koenhabets.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

enum class ActionType {
    login,
    subscribe,
    score,
    endGame
}

enum class ResponseType {
    loginResponse,
    scoreResponse,
    errorResponse
}

@Serializable
data class Message(
    val action: ActionType,
    val data: JsonObject
) {
    @Serializable
    data class Subscribe(val userId: String)

    @Serializable
    data class Score(
        val username: String,
        val game: String,
        val score: Int,
        val fullScore: JsonObject,
        val lastUpdate: Long
    )

    @Serializable
    data class Login(val userId: String, val key: String, val clientVersion: Int)

    @Serializable
    data class EndGame(val game: String, val versionString: String, val versionCode: Int)
}

@Serializable
data class Response(
    val response: ResponseType,
    val data: JsonObject
) {
    @Serializable
    data class LoginResponse(val success: Boolean)

    @Serializable
    data class ScoreResponse(
        val username: String,
        val userId: String,
        val game: String,
        val score: Int,
        val fullScore: JsonObject,
        val lastUpdate: Long
    )

    @Serializable
    data class ErrorResponse(
        val message: String
    )
}
