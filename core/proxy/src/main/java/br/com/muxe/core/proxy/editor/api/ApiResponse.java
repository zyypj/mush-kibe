package br.com.muxe.core.proxy.editor.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record ApiResponse(int status, JsonElement body) {

    public static final String INVALID_REQUEST = "editor.invalid-request";
    public static final String NOT_FOUND = "editor.not-found";
    public static final String METHOD_NOT_ALLOWED = "editor.method-not-allowed";
    public static final String TOO_MANY_REQUESTS = "editor.too-many-requests";
    public static final String TOO_LARGE = "editor.too-large";
    public static final String UNSUPPORTED_MEDIA_TYPE = "editor.unsupported-media-type";
    public static final String BAD_ORIGIN = "editor.bad-origin";
    public static final String UNAVAILABLE = "editor.unavailable";

    private static final int MAX_MESSAGE = 300;

    public static ApiResponse ok(JsonElement body) {
        return new ApiResponse(200, body);
    }

    public static ApiResponse error(int status, String code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message == null ? code : shorten(message));
        JsonObject body = new JsonObject();
        body.add("error", error);
        return new ApiResponse(status, body);
    }

    public String json() {
        return body.toString();
    }

    private static String shorten(String message) {
        return message.length() <= MAX_MESSAGE ? message : message.substring(0, MAX_MESSAGE) + "...";
    }
}
