package nl.koenhabets.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

enum class Action {
    login,
    subscribe,
    score
}

@Serializable
data class Message(
    val action: Action,
    val data: JsonObject
) {
    @Serializable
    data class Subscribe(val userId: String)

    @Serializable
    data class Score(val username: String, val score: Int, val fullScore: JsonObject)

    @Serializable
    data class Login(val userId: String, val key: String, val clientVersion: Int)
}
