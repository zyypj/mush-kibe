package br.com.muxe.core.bukkit.customroom;

import br.com.muxe.core.bukkit.customroom.schema.SchemaReader;
import br.com.muxe.core.common.customroom.schema.RoomSchema;
import br.com.muxe.core.common.network.customroom.CustomRoomCatalogPacket;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CatalogDirectory {

    public static final Duration TIME_TO_LIVE = Duration.ofSeconds(30);

    private final Clock clock;
    private final Logger logger;
    private final Map<String, Entry> byServer = new ConcurrentHashMap<>();
    private final Map<String, Parsed> schemas = new ConcurrentHashMap<>();

    public CatalogDirectory(Clock clock, Logger logger) {
        this.clock = clock;
        this.logger = logger;
    }

    public void update(CustomRoomCatalogPacket packet) {
        byServer.put(packet.serverId(), new Entry(packet, clock.instant()));
    }

    public Optional<CustomRoomCatalogPacket> catalog(String game) {
        return alive(game).stream().max(Comparator.comparing(Entry::at)).map(Entry::packet);
    }

    public List<String> servers(String game) {
        return alive(game).stream().map(entry -> entry.packet().serverId()).sorted().toList();
    }

    public boolean has(String game, String mode) {
        return catalog(game).flatMap(packet -> packet.mode(mode)).isPresent();
    }

    public Optional<RoomSchema> schema(String game) {
        return catalog(game).flatMap(packet -> {
            String text = packet.schemaText();
            Parsed parsed = schemas.get(game);
            if (parsed != null && parsed.text().equals(text)) {
                return Optional.of(parsed.schema());
            }
            try {
                RoomSchema schema = SchemaReader.parse(game, text);
                schemas.put(game, new Parsed(text, schema));
                return Optional.of(schema);
            } catch (RuntimeException exception) {
                logger.log(Level.WARNING, "Could not read the custom room schema of " + game, exception);
                return Optional.empty();
            }
        });
    }

    private List<Entry> alive(String game) {
        Instant oldest = clock.instant().minus(TIME_TO_LIVE);
        return byServer.values().stream().filter(entry -> entry.packet().game().equals(game)).filter(entry -> entry.at().isAfter(oldest)).toList();
    }

    private record Entry(CustomRoomCatalogPacket packet, Instant at) {
    }

    private record Parsed(String text, RoomSchema schema) {
    }
}
