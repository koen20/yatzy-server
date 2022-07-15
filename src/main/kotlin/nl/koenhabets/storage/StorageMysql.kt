package nl.koenhabets.storage

import nl.koenhabets.model.ConfigItem
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

class StorageMysql(configItem: ConfigItem?) {
    lateinit var conn: Connection
    var userDao: UserDao
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
                        if (!conn.isValid(3000)) {
                            conn.close()
                            initStorage()
                        }
                    } catch (e: SQLException) {
                        e.printStackTrace()
                    }
                }
            }, 10000, 60000)
        }
        userDao = UserDaoImpl(conn)
    }

    fun initStorage() {
        conn =
            DriverManager.getConnection(configItem.mysqlServer, configItem.mysqlUsername, configItem.mysqlPassword)
    }

    fun disconnect() {
        try {
            conn.close()
        } catch (_: Exception) {

        }
    }
}
