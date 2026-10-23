import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import javax.inject.Inject

abstract class NetworkExtension @Inject constructor(private val project: Project) {

    fun server(name: String, vararg plugins: ProjectDependency) {
        val declared = project.configurations.dependencyScope("${name}ServerPlugins")
        val resolved = project.configurations.resolvable("${name}ServerPluginJars") {
            extendsFrom(declared.get())
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(SERVER_PLUGIN_USAGE))
            }
        }
        plugins.forEach { project.dependencies.add(declared.name, it) }

        val assembleServer = project.tasks.register<Sync>("assemble${name.replaceFirstChar(Char::uppercaseChar)}Server") {
            group = "muxe"
            description = "Assembles the plugin jars of the $name server into build/servers/$name/plugins."
            from(resolved)
            into(project.layout.buildDirectory.dir("servers/$name/plugins"))
        }
        project.tasks.named("assembleServers") {
            dependsOn(assembleServer)
        }
    }
}
