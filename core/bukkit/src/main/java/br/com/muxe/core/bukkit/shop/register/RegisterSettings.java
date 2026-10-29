package br.com.muxe.core.bukkit.shop.register;

import br.com.muxe.core.bukkit.display.DisplayLabel;
import br.com.muxe.core.bukkit.display.DisplayStand;
import br.com.muxe.core.bukkit.display.LabelStack;
import br.com.muxe.core.bukkit.display.StandMotion;
import br.com.muxe.core.bukkit.display.StandPose;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.npc.NpcSkin;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public record RegisterSettings(double labelBottom, List<Double> labelGaps, double labelSpacing, List<DisplayStand> parts, Optional<NpcSkin> sellerSkin) {

    public static final double DEFAULT_SPACING = 0.28125;

    public RegisterSettings {
        labelGaps = List.copyOf(labelGaps);
        parts = List.copyOf(parts);
    }

    public static RegisterSettings from(ConfigurationSection section) {
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
        ConfigurationSection labels = section.getConfigurationSection("labels");
        Optional<NpcSkin> skin = Optional.ofNullable(section.getString("seller-skin.value"))
            .flatMap(value -> Optional.ofNullable(section.getString("seller-skin.signature")).map(signature -> new NpcSkin(value, signature)));
        return new RegisterSettings(
            labels == null ? 2.25 : labels.getDouble("bottom", 2.25),
            labels == null ? List.of() : labels.getDoubleList("gaps"),
            labels == null ? DEFAULT_SPACING : labels.getDouble("spacing", DEFAULT_SPACING),
            parts,
            skin
        );
    }

    public List<DisplayLabel> labels(List<String> lines) {
        return LabelStack.upward(lines, labelBottom, labelGaps, labelSpacing);
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
