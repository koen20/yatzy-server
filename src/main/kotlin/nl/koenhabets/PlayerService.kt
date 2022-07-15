package nl.koenhabets

import io.ktor.util.date.*
import nl.koenhabets.model.Player

class PlayerService {
    val players: MutableMap<String, Player> = mutableMapOf()

    init {
        // todo auto cleanup
    }

    /** Create player if the id doesn't exist
     * @param playerId id of the player to add */
    fun addPlayer(playerId: String) {
        if (!players.contains(playerId)) {
            players[playerId] = Player(getTimeMillis())
        }
    }

    fun addSubscription(playerId: String, subPlayerId: String) {
        addPlayer(playerId)
        players[playerId]!!.subscriptions.add(subPlayerId)
    }

    fun getSubscriptions(playerId: String): ArrayList<String> {
        return players[playerId]!!.subscriptions
    }
}
