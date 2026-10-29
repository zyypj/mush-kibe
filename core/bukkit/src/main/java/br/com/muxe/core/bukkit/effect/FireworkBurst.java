package br.com.muxe.core.bukkit.effect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;

public final class FireworkBurst {

    public static final long DETONATE_AFTER_TICKS = 2L;

    private static final byte EXPLODE_STATUS = 17;

    private FireworkBurst() {
    }

    public static void explode(Plugin plugin, Location at, FireworkEffect effect) {
        World world = Objects.requireNonNull(at.getWorld(), "world");
        Firework firework = world.spawn(at, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.clearEffects();
        meta.addEffect(effect);
        meta.setPower(0);
        firework.setFireworkMeta(meta);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (firework.isValid() && Holder.HANDLES.map(handles -> handles.detonate(firework)).orElse(false)) {
                firework.remove();
            }
        }, DETONATE_AFTER_TICKS);
    }

    public static FireworkEffect effect(ConfigurationSection section) {
        List<Color> colors = colors(section.getStringList("colors"));
        if (colors.isEmpty()) {
            throw new IllegalArgumentException(section.getCurrentPath() + ".colors needs at least one color");
        }
        return FireworkEffect.builder()
            .with(FireworkEffect.Type.valueOf(section.getString("type", "BALL_LARGE").toUpperCase(Locale.ROOT)))
            .withColor(colors)
            .withFade(colors(section.getStringList("fade-colors")))
            .flicker(section.getBoolean("flicker", false))
            .trail(section.getBoolean("trail", false))
            .build();
    }

    public static Color color(String hex) {
        String digits = hex.startsWith("#") ? hex.substring(1) : hex;
        if (digits.length() != 6) {
            throw new IllegalArgumentException("Colors are written as #RRGGBB, got: " + hex);
        }
        return Color.fromRGB(Integer.parseInt(digits, 16));
    }

    private static List<Color> colors(List<String> values) {
        List<Color> colors = new ArrayList<>(values.size());
        values.forEach(value -> colors.add(color(value)));
        return colors;
    }

    private static final class Holder {
        private static final Optional<Handles> HANDLES = Handles.resolve();
    }

    private static final class Handles {

        private final Method handle;
        private final Method broadcast;

        private Handles(Method handle, Method broadcast) {
            this.handle = handle;
            this.broadcast = broadcast;
        }

        private static Optional<Handles> resolve() {
            try {
                String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
                String version = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
                String nms = "net.minecraft.server." + version + ".";
                Class<?> entity = Class.forName(nms + "Entity");
                return Optional.of(new Handles(
                    Class.forName(craftPackage + ".entity.CraftEntity").getMethod("getHandle"),
                    Class.forName(nms + "World").getMethod("broadcastEntityEffect", entity, byte.class)
                ));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                Bukkit.getLogger().log(Level.WARNING, "[Muxe] Fireworks will fly before exploding on this server: " + exception);
                return Optional.empty();
            }
        }

        private boolean detonate(Firework firework) {
            try {
                Object entity = handle.invoke(firework);
                Object world = entity.getClass().getField("world").get(entity);
                broadcast.invoke(world, entity, EXPLODE_STATUS);
                return true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not detonate a firework", exception);
                return false;
            }
        }
    }
}
