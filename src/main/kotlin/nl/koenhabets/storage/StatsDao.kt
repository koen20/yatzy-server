package nl.koenhabets.storage

import java.sql.Connection
import java.sql.SQLException
import java.sql.Timestamp
import java.util.*

interface StatsDao {
    fun addItem(statId: Int, value: Int): Boolean
}

class StatsDaoImpl(private val conn: Connection?) : StatsDao {
    override fun addItem(statId: Int, value: Int): Boolean {
        try {
            conn?.let { connection ->
                val insert = "INSERT INTO stats VALUES(null, ?, ?, ?)"
                val pst = connection.prepareStatement(insert)
                pst.use { ps ->
                    ps.setTimestamp(1, Timestamp(Date().time))
                    ps.setInt(2, statId)
                    ps.setInt(3, value)
                    return ps.execute()
                }
            }
        } catch (exception: SQLException) {
            exception.printStackTrace()
        }
        return false
    }
}
