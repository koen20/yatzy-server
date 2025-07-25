package nl.koenhabets.plugins

import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import nl.koenhabets.SocketHandler
import nl.koenhabets.StatsCollector
import nl.koenhabets.model.Connection
import nl.koenhabets.storage.StorageMysql
import java.util.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets(storage: StorageMysql, statsCollector: StatsCollector) {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
    }

    routing {
        val connections = Collections.synchronizedSet<Connection>(LinkedHashSet())
        val json = Json {
            ignoreUnknownKeys = true
        }
        webSocket("api/v1/ws") {
            val thisConnection = Connection(this)
            connections += thisConnection
            statsCollector.setWsConnected(connections.size)

            try {
                val socketHandler = SocketHandler(thisConnection, connections, storage, json)
                for (frame in incoming) {
                    socketHandler.handleMessage(frame)
                }
            } finally {
                connections -= thisConnection
                statsCollector.setWsConnected(connections.size)
            }
        }
    }
}
