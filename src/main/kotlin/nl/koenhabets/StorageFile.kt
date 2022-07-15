package nl.koenhabets

import nl.koenhabets.model.User
import java.io.*


class StorageFile {
    private var users: ArrayList<User> = ArrayList()

    init {
        //users = readFromFile()
    }

    fun addPlayer(user: User) {
        users.add(user)
        updateStorage()
    }

    fun removePlayer(user: User) {
        users.remove(user)
        updateStorage()
    }

    // Check if userId and key is correct
    fun checkPlayer(userId: String, key: String): Boolean {
        if (users.find { it.userId == userId && it.key == key } != null) {
            return true
        }
        return false
    }

    fun playerExists(userId: String): Boolean {
        if (users.find { it.userId == userId } != null) {
            return true
        }
        return false
    }

    private fun updateStorage() {
        //saveToFile(users)
    }

    @Synchronized
    private fun saveToFile(list: ArrayList<User>) {
        try {
            val outputStream = ObjectOutputStream(FileOutputStream("players"))
            outputStream.writeObject(list)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun readFromFile(): ArrayList<User> {
        var res = ArrayList<User>()
        try {
            val inputStream = ObjectInputStream(FileInputStream("players"))
            res = inputStream.readObject() as ArrayList<User>
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        return res
    }
}
