package nl.koenhabets.model

import kotlinx.serialization.Serializable
@Serializable
class ConfigItem(
    val mysqlServer: String,
    val mysqlUsername: String,
    val mysqlPassword: String
)
