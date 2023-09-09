package nl.koenhabets.model

import io.ktor.websocket.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class Connection(val session: DefaultWebSocketSession) {
    var loggedIn: Boolean = false
    var userId: String? = null
    val subscriptions: ArrayList<String> = ArrayList()
    var highestScoreCount: Int = 0
    var lastScoreResponse: Response.ScoreResponse? = null

    fun updateHighestScoreCount(fullScore: JsonObject) {
        val scoreCount = getScoreCount(fullScore)
        if (scoreCount > highestScoreCount) {
            highestScoreCount = scoreCount
        }
    }

    fun getScoreCount(fullScore: JsonObject): Int {
        var count = 0
        fullScore.forEach {
            if (it.value.jsonPrimitive.content != "") {
                count++
            }
        }
        return count
    }
}
