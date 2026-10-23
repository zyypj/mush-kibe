package br.com.muxe.core.proxy.config;

import br.com.muxe.core.common.config.ConfigException;
import br.com.muxe.core.common.config.source.MapConfigSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public final class ProxyConfigFile {

    public static final String FILE_NAME = "config.yml";

    private ProxyConfigFile() {
    }

    public static MapConfigSource load(Path dataDirectory) {
        Path file = dataDirectory.resolve(FILE_NAME);
        try {
            if (Files.notExists(file)) {
                copyDefault(file);
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Object root = new Yaml().load(reader);
                if (!(root instanceof Map<?, ?> map)) {
                    throw new ConfigException(file + " must contain a YAML map");
                }
                return new MapConfigSource(map);
            }
        } catch (IOException exception) {
            throw new ConfigException("Cannot read " + file, exception);
        }
    }

    private static void copyDefault(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (InputStream defaults = ProxyConfigFile.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (defaults == null) {
                throw new ConfigException("Default " + FILE_NAME + " is missing from the plugin jar");
            }
            Files.copy(defaults, file);
        }
    }
}
