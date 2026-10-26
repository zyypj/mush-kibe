package br.com.muxe.core.bukkit.display;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Location;

public record DisplayStand(
    double x,
    double y,
    double z,
    float yaw,
    boolean small,
    boolean arms,
    Optional<ItemBuilder> helmet,
    Optional<ItemBuilder> hand,
    StandPose headPose,
    StandPose rightArmPose,
    StandMotion motion
) {

    public DisplayStand {
        Objects.requireNonNull(helmet, "helmet");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(headPose, "headPose");
        Objects.requireNonNull(rightArmPose, "rightArmPose");
        Objects.requireNonNull(motion, "motion");
    }

    public Location at(Location anchor, long tick) {
        return new Location(anchor.getWorld(), anchor.getX() + x, anchor.getY() + y + motion.lift(tick), anchor.getZ() + z, motion.yaw(yaw, tick), 0f);
    }
}
