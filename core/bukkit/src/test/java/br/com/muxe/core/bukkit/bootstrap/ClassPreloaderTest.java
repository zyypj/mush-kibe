package br.com.muxe.core.bukkit.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ClassPreloaderTest {

    @Test
    void loadsEveryClassOfThePackageUpFront() {
        RecordingLoader loader = new RecordingLoader();

        int loaded = ClassPreloader.preload(loader, Stream.of(
            "br/com/muxe/core/common/cosmetic/domain/CosmeticProfile.class",
            "br/com/muxe/core/bukkit/text/Title.class",
            "br/com/muxe/libs/hikari/HikariConfig.class",
            "plugin.yml",
            "META-INF/versions/9/module-info.class"), "br.com.muxe.core");

        assertThat(loaded).isEqualTo(2);
        assertThat(loader.requested).containsExactly(
            "br.com.muxe.core.common.cosmetic.domain.CosmeticProfile",
            "br.com.muxe.core.bukkit.text.Title");
    }

    @Test
    void classesThatCannotBeLoadedAreSkipped() {
        RecordingLoader loader = new RecordingLoader();

        int loaded = ClassPreloader.preload(loader, Stream.of(
            "br/com/muxe/core/Missing.class",
            "br/com/muxe/core/bukkit/text/Title.class"), "br.com.muxe.core");

        assertThat(loaded).isEqualTo(1);
    }

    private static final class RecordingLoader extends URLClassLoader {

        private final List<String> requested = new ArrayList<>();

        private RecordingLoader() {
            super(new URL[0], ClassPreloaderTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("br.com.muxe.core.")) {
                requested.add(name);
            }
            return super.loadClass(name, resolve);
        }
    }
}
