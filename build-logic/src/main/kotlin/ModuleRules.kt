object ModuleRules {

    private val allowedDependencies: Map<String, List<String>> = linkedMapOf(
        ":core:common" to listOf(":core:common"),
        ":core:bukkit" to listOf(":core:common"),
        ":core:proxy" to listOf(":core:common"),
        ":minigames:engine" to listOf(":core:common", ":core:bukkit"),
        ":minigames" to listOf(":core:common", ":core:bukkit", ":minigames:engine"),
        ":servers" to listOf(":core:common", ":core:bukkit"),
    )

    fun allowedFor(module: String): List<String>? =
        allowedDependencies.entries.firstOrNull { belongsTo(module, it.key) }?.value

    fun belongsTo(module: String, group: String): Boolean =
        module == group || module.startsWith("$group:")
}
