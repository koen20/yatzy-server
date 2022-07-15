package nl.koenhabets.model

import io.ktor.websocket.*

class Connection(val session: DefaultWebSocketSession) {
    var loggedIn: Boolean = false
    var userId: String? = null
    val subscriptions: ArrayList<String> = ArrayList()
}
