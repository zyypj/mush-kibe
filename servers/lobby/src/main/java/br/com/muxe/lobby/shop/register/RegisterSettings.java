package br.com.muxe.lobby.shop.register;

import br.com.muxe.core.bukkit.display.DisplayLabel;
import br.com.muxe.core.bukkit.display.DisplayStand;
import br.com.muxe.core.bukkit.display.StandMotion;
import br.com.muxe.core.bukkit.display.StandPose;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.npc.NpcSkin;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public record RegisterSettings(List<DisplayLabel> labels, List<DisplayStand> parts, boolean seller, Optional<NpcSkin> sellerSkin) {

    public RegisterSettings {
        labels = List.copyOf(labels);
        parts = List.copyOf(parts);
    }

    public static RegisterSettings from(ConfigurationSection section) {
        List<DisplayLabel> labels = new ArrayList<>();
        ConfigurationSection labelSection = section.getConfigurationSection("labels");
        if (labelSection != null) {
            for (String key : labelSection.getKeys(false)) {
                ConfigurationSection label = labelSection.getConfigurationSection(key);
                if (label != null) {
                    labels.add(new DisplayLabel(label.getString("text", ""), label.getDouble("height")));
                }
            }
        }
        List<DisplayStand> parts = new ArrayList<>();
        ConfigurationSection partSection = section.getConfigurationSection("parts");
        if (partSection != null) {
            for (String key : partSection.getKeys(false)) {
                ConfigurationSection part = partSection.getConfigurationSection(key);
                if (part != null) {
                    parts.add(part(part));
                }
            }
        }
        ConfigurationSection seller = section.getConfigurationSection("seller");
        Optional<NpcSkin> skin = Optional.ofNullable(seller)
            .flatMap(sellerSection -> Optional.ofNullable(sellerSection.getString("skin.value"))
                .flatMap(value -> Optional.ofNullable(sellerSection.getString("skin.signature")).map(signature -> new NpcSkin(value, signature))));
        return new RegisterSettings(labels, parts, seller != null && seller.getBoolean("enabled", true), skin);
    }

    private static DisplayStand part(ConfigurationSection part) {
        return new DisplayStand(
            part.getDouble("x"),
            part.getDouble("y"),
            part.getDouble("z"),
            (float) part.getDouble("yaw"),
            part.getBoolean("small", false),
            part.getBoolean("arms", false),
            item(part, "helmet"),
            item(part, "hand"),
            pose(part, "head-pose"),
            pose(part, "right-arm-pose"),
            motion(part.getConfigurationSection("motion"))
        );
    }

    private static Optional<ItemBuilder> item(ConfigurationSection part, String path) {
        return Optional.ofNullable(part.getConfigurationSection(path)).map(ItemBuilder::from);
    }

    private static StandPose pose(ConfigurationSection part, String path) {
        ConfigurationSection pose = part.getConfigurationSection(path);
        if (pose == null) {
            return StandPose.ZERO;
        }
        return new StandPose(pose.getDouble("x"), pose.getDouble("y"), pose.getDouble("z"));
    }

    private static StandMotion motion(ConfigurationSection motion) {
        if (motion == null) {
            return StandMotion.NONE;
        }
        return new StandMotion(
            motion.getDouble("spin"),
            motion.getDouble("bob-height"),
            motion.getInt("bob-period"),
            motion.getDouble("phase")
        );
    }
}
