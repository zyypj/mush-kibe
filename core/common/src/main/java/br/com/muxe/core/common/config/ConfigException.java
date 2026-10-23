package br.com.muxe.core.common.config;

public final class ConfigException extends RuntimeException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ConfigException missing(String path) {
        return new ConfigException("Missing required configuration '" + path + "'");
    }

    public static ConfigException invalid(String path, Object value, String expected) {
        return new ConfigException("Configuration '" + path + "' must be " + expected + ", got: " + value);
    }
}
