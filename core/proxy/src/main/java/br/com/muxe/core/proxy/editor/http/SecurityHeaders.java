package br.com.muxe.core.proxy.editor.http;

import com.sun.net.httpserver.Headers;

public final class SecurityHeaders {

    public static final String CONTENT_SECURITY_POLICY = String.join("; ",
        "default-src 'self'",
        "script-src 'self'",
        "style-src 'self' https://fonts.googleapis.com",
        "img-src 'self' data:",
        "connect-src 'self'",
        "font-src 'self' https://fonts.gstatic.com",
        "object-src 'none'",
        "base-uri 'none'",
        "form-action 'none'",
        "frame-ancestors 'none'"
    );

    private SecurityHeaders() {
    }

    public static void apply(Headers headers, boolean api) {
        headers.set("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        headers.set("X-Frame-Options", "DENY");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("Cross-Origin-Opener-Policy", "same-origin");
        headers.set("Cross-Origin-Resource-Policy", "same-origin");
        headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=(), usb=()");
        headers.set("Cache-Control", api ? "no-store" : "no-cache");
        if (api) {
            headers.set("Pragma", "no-cache");
        }
    }
}
