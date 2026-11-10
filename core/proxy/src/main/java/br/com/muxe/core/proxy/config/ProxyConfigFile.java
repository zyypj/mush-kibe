package br.com.muxe.core.proxy.config;

import br.com.muxe.core.common.config.ConfigException;
import br.com.muxe.core.common.config.source.ConfigSource;
import br.com.muxe.core.common.config.source.MapConfigSource;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.proxy.language.LocalizedConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public final class ProxyConfigFile {

    public static final String FILE_NAME = "config.yml";

    private ProxyConfigFile() {
    }

    public static MapConfigSource load(Path dataDirectory) {
        return load(dataDirectory, FILE_NAME);
    }

    public static LocalizedConfig loadTexts(Path dataDirectory, ConfigSource standard) {
        Map<Language, ConfigSource> translations = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            if (language != Language.DEFAULT) {
                String resource = LocalizedConfig.resource(language);
                translations.put(language, LocalizedConfig.layered(load(dataDirectory, resource), bundled(resource)));
            }
        }
        return LocalizedConfig.of(standard, translations);
    }

    public static MapConfigSource bundled(String resource) {
        try (InputStream stream = defaults(resource); Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return read(reader, resource);
        } catch (IOException exception) {
            throw new ConfigException("Cannot read the default " + resource + " from the plugin jar", exception);
        }
    }

    private static MapConfigSource load(Path dataDirectory, String resource) {
        Path file = dataDirectory.resolve(resource);
        try {
            if (Files.notExists(file)) {
                copyDefault(file, resource);
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                return read(reader, file.toString());
            }
        } catch (IOException exception) {
            throw new ConfigException("Cannot read " + file, exception);
        }
    }

    private static MapConfigSource read(Reader reader, String name) {
        Object root = new Yaml().load(reader);
        if (!(root instanceof Map<?, ?> map)) {
            throw new ConfigException(name + " must contain a YAML map");
        }
        return new MapConfigSource(map);
    }

    private static void copyDefault(Path file, String resource) throws IOException {
        Files.createDirectories(file.getParent());
        try (InputStream defaults = defaults(resource)) {
            Files.copy(defaults, file);
        }
    }

    private static InputStream defaults(String resource) {
        InputStream defaults = ProxyConfigFile.class.getClassLoader().getResourceAsStream(resource);
        if (defaults == null) {
            throw new ConfigException("Default " + resource + " is missing from the plugin jar");
        }
        return defaults;
    }
}
