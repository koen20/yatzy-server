package nl.koenhabets.storage

import nl.koenhabets.model.User
import java.sql.Connection

interface UserDao {
    /** Create user if it doens't exist and checks if the userId and key are correct */
    fun checkUser(userId: String, key: String): Boolean
}

class UserDaoImpl (conn: Connection?) : UserDao {
    private var users: ArrayList<User> = ArrayList()

    override fun checkUser(userId: String, key: String): Boolean {
        TODO("Not yet implemented")
    }
}
