package br.com.muxe.core.common.config.secret;

import br.com.muxe.core.common.config.ConfigException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public final class SecretResolver {

    public static final String ENV_PREFIX = "env:";
    public static final String FILE_PREFIX = "file:";

    private final Function<String, String> environment;

    public SecretResolver(Function<String, String> environment) {
        this.environment = environment;
    }

    public static SecretResolver system() {
        return new SecretResolver(System::getenv);
    }

    public String resolve(String raw) {
        if (raw == null) {
            return null;
        }
        if (raw.startsWith(ENV_PREFIX)) {
            String name = raw.substring(ENV_PREFIX.length());
            String value = environment.apply(name);
            if (value == null) {
                throw new ConfigException("Environment variable " + name + " is not set");
            }
            return value;
        }
        if (raw.startsWith(FILE_PREFIX)) {
            Path file = Path.of(raw.substring(FILE_PREFIX.length()));
            try {
                return Files.readString(file, StandardCharsets.UTF_8).strip();
            } catch (IOException exception) {
                throw new ConfigException("Cannot read secret file " + file, exception);
            }
        }
        return raw;
    }
}
