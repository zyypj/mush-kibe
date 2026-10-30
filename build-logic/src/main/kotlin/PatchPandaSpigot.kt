import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import javax.inject.Inject

abstract class PatchPandaSpigot : DefaultTask() {

    @get:Input
    abstract val build: Property<String>

    @get:Input
    abstract val sha256: Property<String>

    @get:Internal
    abstract val workDirectory: DirectoryProperty

    @get:Internal
    abstract val patchedJar: RegularFileProperty

    @get:OutputFile
    abstract val ready: RegularFileProperty

    @get:Nested
    abstract val launcher: Property<JavaLauncher>

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun patch() {
        val directory = workDirectory.get().asFile
        val patched = patchedJar.get().asFile
        val marker = File(directory, "patched.sha256")
        if (patched.isFile && marker.isFile && marker.readText() == sha256.get()) {
            ready.get().asFile.writeText(patched.absolutePath)
            return
        }
        directory.mkdirs()
        val paperclip = File(directory, "pandaspigot-${build.get()}.jar")
        if (!paperclip.isFile || digest(paperclip) != sha256.get()) {
            download(paperclip)
        }
        execOperations.exec {
            workingDir = directory
            executable = launcher.get().executablePath.asFile.absolutePath
            args("-Dpaperclip.patchonly=true", "-jar", paperclip.absolutePath)
        }
        if (!patched.isFile) {
            throw GradleException("PandaSpigot ${build.get()} did not produce ${patched.absolutePath}")
        }
        marker.writeText(sha256.get())
        ready.get().asFile.writeText(patched.absolutePath)
    }

    private fun download(target: File) {
        val url = "https://downloads.hpfxd.com/v2/projects/pandaspigot/versions/1.8.8/builds/${build.get()}/downloads/paperclip"
        val partial = File(target.parentFile, target.name + ".part")
        URI.create(url).toURL().openStream().use { input ->
            Files.copy(input, partial.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        val actual = digest(partial)
        if (actual != sha256.get()) {
            partial.delete()
            throw GradleException("PandaSpigot ${build.get()} has sha256 $actual, expected ${sha256.get()}")
        }
        Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private fun digest(file: File): String {
        val sha = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) {
                    break
                }
                sha.update(buffer, 0, read)
            }
        }
        return sha.digest().joinToString("") { "%02x".format(it) }
    }
}
