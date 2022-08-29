package nl.koenhabets.storage

import nl.koenhabets.model.ConfigItem
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

class StorageMysql(configItem: ConfigItem?) {
    var conn: Connection? = null
    var userDao: UserDao
    var gameDao: GameDao
    var statsDao: StatsDao
    private lateinit var configItem: ConfigItem

    init {
        if (configItem !== null) {
            this.configItem = configItem
            try {
                initStorage()
            } catch (e: Exception) {
                println("Failed to connect to database $e")
            }

            Timer().scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    try {
                        if (conn?.isValid(3) != true) {
                            conn?.close()
                            initStorage()
                        }
                    } catch (e: SQLException) {
                        e.printStackTrace()
                    }
                }
            }, 10000, 60000)
        } else {
            println("Config doesn't exist. Storage is disabled")
        }
        userDao = UserDaoImpl(conn)
        gameDao = GameDaoImpl(conn)
        statsDao = StatsDaoImpl(conn)
    }

    fun initStorage() {
        conn =
            DriverManager.getConnection(configItem.mysqlServer, configItem.mysqlUsername, configItem.mysqlPassword)
    }

    fun disconnect() {
        try {
            conn?.close()
        } catch (_: Exception) {

        }
    }
}
