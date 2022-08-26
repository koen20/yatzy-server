package nl.koenhabets.storage

import nl.koenhabets.model.Message
import java.sql.Connection
import java.sql.SQLException
import java.sql.Timestamp
import java.util.*

interface GameDao {
    fun addGame(game: Message.EndGame): Boolean
}

class GameDaoImpl(private val conn: Connection?) : GameDao {
    override fun addGame(game: Message.EndGame): Boolean {
        println("Saving game")
        try {
            conn?.let { connection ->
                val insert = "INSERT INTO games VALUES(null, ?, ?, ?, ?)"
                val pst = connection.prepareStatement(insert)
                pst.use { ps ->
                    ps.setTimestamp(1, Timestamp(Date().time))
                    ps.setString(2, game.game)
                    ps.setInt(3, game.versionCode)
                    ps.setString(4, game.versionString)
                    return ps.execute()
                }
            }
        } catch (exception: SQLException) {
            exception.printStackTrace()
        }
        return false
    }
}
