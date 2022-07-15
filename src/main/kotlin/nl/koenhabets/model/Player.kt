package nl.koenhabets.model

class Player(var lastSeen: Long) {
    val subscriptions: ArrayList<String> = ArrayList()
    var username: String? = null
}
