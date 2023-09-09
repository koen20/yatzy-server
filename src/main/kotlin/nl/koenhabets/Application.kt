package nl.koenhabets

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import nl.koenhabets.model.ConfigItem
import nl.koenhabets.plugins.configureSockets
import nl.koenhabets.storage.StorageMysql
import java.io.File

fun main() {
    var configItem: ConfigItem? = null


    try {
        configItem = Json.decodeFromString<ConfigItem>(File("yatzy-config.json").readText(Charsets.UTF_8))
    } catch (e: Exception) {
        e.printStackTrace()
    }

    val storage = StorageMysql(configItem)
    val statsCollector = StatsCollector(storage.statsDao)

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        //configureRouting()
        //configureSecurity()
        configureSockets(storage, statsCollector)
    }.start(wait = true)
}
