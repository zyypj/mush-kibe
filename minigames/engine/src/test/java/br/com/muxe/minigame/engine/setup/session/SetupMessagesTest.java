package br.com.muxe.minigame.engine.setup.session;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SetupMessagesTest {

    private static final Pattern KEY = Pattern.compile("\"(setup-[a-z0-9-]*[a-z0-9])\"");

    @Test
    void everyKeyTheSetupUsesHasADefaultMessage() throws IOException {
        MessageCatalog messages = SetupMessages.defaults();
        Set<String> used = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/br/com/muxe/minigame/engine/setup"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = KEY.matcher(Files.readString(file));
                while (matcher.find()) {
                    used.add(matcher.group(1));
                }
            }
        }
        used.add("setup-help-hint-location");
        used.add("setup-help-hint-center");
        used.add("setup-help-hint-block");
        used.add("setup-help-hint-height");
        used.add("setup-help-hint-region");

        assertThat(used).isNotEmpty().allSatisfy(key -> assertThat(messages.get(key)).as(key).doesNotContain("Missing message"));
    }

    @Test
    void thePluginCanOverrideAnyDefault() {
        MessageCatalog merged = SetupMessages.withOverrides(MessageCatalog.of(Map.of("setup-left", "&7Até mais.")));

        assertThat(merged.get("setup-left")).isEqualTo("§7Até mais.");
        assertThat(merged.get("setup-not-editing")).contains("/arena criar");
    }
}
