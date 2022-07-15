package nl.koenhabets.websocket

import nl.koenhabets.StorageFile
import nl.koenhabets.model.Connection
import nl.koenhabets.model.Message
import nl.koenhabets.model.User

class Login(login: Message.Login, connection: Connection, storageFile: StorageFile) {
    init {
        val userId = login.userId
        val key = login.key
        if (storageFile.checkPlayer(userId, key)) {
            connection.userId = userId
            connection.loggedIn = true
            //players[userId] = Player(time)
        } else {
            storageFile.addPlayer(User(userId, key))
            connection.userId = userId
            connection.loggedIn = true
            //players[userId] = Player(time)
        }
    }
}
