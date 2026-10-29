package br.com.muxe.core.common.permission.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Lifetime(Optional<Duration> duration) {

    public static final Lifetime PERMANENT = new Lifetime(Optional.empty());
    public static final List<String> PERMANENT_WORDS = List.of("permanente", "vitalicio", "vitalício");
    public static final List<String> SUGGESTIONS = List.of("permanente", "1h", "12h", "1d", "7d", "15d", "30d", "60d", "90d", "365d");

    private static final Pattern PART = Pattern.compile("(\\d{1,6})([smhdw])");

    public Lifetime {
        Objects.requireNonNull(duration, "duration");
        if (duration.isPresent() && (duration.get().isNegative() || duration.get().isZero())) {
            throw new IllegalArgumentException("A lifetime needs a positive duration, got: " + duration.get());
        }
    }

    public static Lifetime of(Duration duration) {
        return new Lifetime(Optional.of(duration));
    }

    public static Optional<Lifetime> parse(String text) {
        String clean = Objects.requireNonNull(text, "text").trim().toLowerCase(Locale.ROOT);
        if (PERMANENT_WORDS.contains(clean)) {
            return Optional.of(PERMANENT);
        }
        Matcher matcher = PART.matcher(clean);
        int end = 0;
        Duration total = Duration.ZERO;
        while (matcher.find()) {
            if (matcher.start() != end) {
                return Optional.empty();
            }
            total = total.plus(unit(matcher.group(2)).multipliedBy(Long.parseLong(matcher.group(1))));
            end = matcher.end();
        }
        if (clean.isEmpty() || end != clean.length() || total.isZero()) {
            return Optional.empty();
        }
        return Optional.of(of(total));
    }

    public static boolean looksLike(String text) {
        return parse(text).isPresent();
    }

    public boolean isPermanent() {
        return duration.isEmpty();
    }

    public Optional<Instant> expiryAfter(Optional<PermissionNode> current, Instant now) {
        if (duration.isEmpty()) {
            return Optional.empty();
        }
        Instant base = current
            .filter(node -> node.value() && node.isActive(now))
            .flatMap(PermissionNode::expiresAt)
            .orElse(now);
        return Optional.of(base.plus(duration.get()));
    }

    private static Duration unit(String unit) {
        return switch (unit) {
            case "s" -> Duration.ofSeconds(1);
            case "m" -> Duration.ofMinutes(1);
            case "h" -> Duration.ofHours(1);
            case "d" -> Duration.ofDays(1);
            case "w" -> Duration.ofDays(7);
            default -> throw new IllegalStateException("Unreachable unit " + unit);
        };
    }
}
