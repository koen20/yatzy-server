package nl.koenhabets

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import nl.koenhabets.plugins.*
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import nl.koenhabets.model.ConfigItem
import nl.koenhabets.storage.StorageMysql

fun main() {
    var configItem: ConfigItem? = null


    try {
        configItem = Json.decodeFromString<ConfigItem>(File("yatzy-config.json").readText(Charsets.UTF_8))
    } catch (e: Exception) {
        e.printStackTrace()
    }

    val storage = StorageMysql(configItem)


    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        //configureRouting()
        //configureSecurity()
        configureSockets(storage)
    }.start(wait = true)
}
