const val SERVER_PLUGIN_USAGE = "muxe-server-plugin"

object PluginJars {

    fun nameFor(projectPath: String): String {
        val segments = projectPath.split(':').filter { it.isNotEmpty() }
        val name = if (segments.first() == "core") segments.joinToString("-") else segments.last()
        return "muxe-$name"
    }
}

object Relocations {

    const val TARGET = "br.com.muxe.libs"

    val shared = listOf(
        "com.zaxxer.hikari",
        "org.postgresql",
        "org.flywaydb",
        "redis.clients",
        "org.apache.commons.pool2",
        "org.apache.commons.text",
        "org.apache.commons.lang3",
        "org.json",
        "com.fasterxml.jackson",
    )

    val bukkitOnly = listOf(
        "com.google.gson",
        "org.slf4j",
    )

    val annotationOnly = listOf(
        "org.checkerframework:.*",
        "com.google.errorprone:.*",
    )

    val providedByVelocity = listOf(
        "com.google.code.gson:gson",
        "org.slf4j:slf4j-api",
    )
}
