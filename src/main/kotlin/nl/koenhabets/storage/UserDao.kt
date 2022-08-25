package nl.koenhabets.storage

import nl.koenhabets.model.User
import java.sql.Connection
import java.sql.SQLException

interface UserDao {
    /** Create user if it doens't exist and checks if the userId and key are correct */
    fun checkUser(userId: String, key: String): Boolean
}

class UserDaoImpl(private val conn: Connection?) : UserDao {
    private var users: ArrayList<User> = ArrayList()

    override fun checkUser(userId: String, key: String): Boolean {
        if (users.size == 0) {
            users = getAllUsers()
        }
        println(users.size)

        val user = getUser(userId)

        return if (user == null) {
            addUser(User(userId, key))
            true
        } else {
            if (user.key != key) {
                println("Incorrect key for user: $userId")
            }
            user.key == key
        }
    }

    private fun getUser(userId: String): User? {
        return users.find { it.userId == userId }
    }

    private fun addUser(user: User): Boolean {
        println("Adding user ${user.userId}")
        if (getUser(user.userId) == null) {
            users.add(user)
        }
        try {
            conn?.let { connection ->
                val insert = "INSERT INTO users VALUES(?, ?)"
                val pst = connection.prepareStatement(insert)
                pst.use { ps ->
                    ps.setString(1, user.userId)
                    ps.setString(2, user.key)
                    return ps.execute()
                }
            }
        } catch (exception: SQLException) {
            exception.printStackTrace()
        }
        return false
    }

    private fun getAllUsers(): ArrayList<User> {
        val items: ArrayList<User> = ArrayList()
        try {
            conn?.let { connection ->
                val query = "SELECT * FROM users"
                connection.createStatement().use { stmt ->
                    stmt.executeQuery(query).use { rs ->
                        while (rs.next()) {
                            val user = User(
                                rs.getString("userId"),
                                rs.getString("key"),
                            )
                            items.add(user)
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return items
    }
}
