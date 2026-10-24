package br.com.muxe.core.bukkit.nms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;

public final class SkullTextures {

    private SkullTextures() {
    }

    public static boolean apply(SkullMeta meta, String texture) {
        return Holder.HANDLES.map(handles -> handles.apply(meta, texture)).orElse(false);
    }

    private static final class Holder {
        private static final Optional<Handles> HANDLES = Handles.resolve();
    }

    private static final class Handles {

        private final Constructor<?> profile;
        private final Method properties;
        private final Constructor<?> property;

        private Handles() throws ReflectiveOperationException {
            Class<?> profileType = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyType = Class.forName("com.mojang.authlib.properties.Property");
            this.profile = profileType.getConstructor(UUID.class, String.class);
            this.properties = profileType.getMethod("getProperties");
            this.property = propertyType.getConstructor(String.class, String.class);
        }

        private static Optional<Handles> resolve() {
            try {
                return Optional.of(new Handles());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                Bukkit.getLogger().log(Level.WARNING, "[Muxe] Skull textures are unavailable on this server: " + exception);
                return Optional.empty();
            }
        }

        private boolean apply(SkullMeta meta, String texture) {
            try {
                Object gameProfile = profile.newInstance(UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8)), null);
                Object map = properties.invoke(gameProfile);
                map.getClass().getMethod("put", Object.class, Object.class).invoke(map, "textures", property.newInstance("textures", texture));
                Field field = meta.getClass().getDeclaredField("profile");
                field.setAccessible(true);
                field.set(meta, gameProfile);
                return true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                Bukkit.getLogger().log(Level.FINE, "[Muxe] Could not apply a skull texture", exception);
                return false;
            }
        }
    }
}
