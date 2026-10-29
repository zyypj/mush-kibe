package br.com.muxe.core.bukkit.npc.placed;

import br.com.muxe.core.common.placement.domain.NpcAction;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public final class NpcLines {

    private final Map<NpcAction.Type, List<String>> templates;

    public NpcLines(Map<NpcAction.Type, List<String>> templates) {
        this.templates = new EnumMap<>(NpcAction.Type.class);
        templates.forEach((type, lines) -> this.templates.put(type, List.copyOf(lines)));
    }

    public static NpcLines from(ConfigurationSection lines) {
        Map<NpcAction.Type, List<String>> templates = new EnumMap<>(NpcAction.Type.class);
        if (lines != null) {
            for (NpcAction.Type type : NpcAction.Type.values()) {
                templates.put(type, lines.getStringList(type.name().toLowerCase(Locale.ROOT)));
            }
        }
        return new NpcLines(templates);
    }

    public List<String> render(NpcPlacement npc, int players) {
        List<String> lines = npc.hasOwnLines() ? npc.lines() : templates.getOrDefault(npc.action().type(), List.of());
        return lines.stream()
            .map(line -> line.replace("{name}", npc.name()).replace("{players}", Integer.toString(players)))
            .toList();
    }
}
