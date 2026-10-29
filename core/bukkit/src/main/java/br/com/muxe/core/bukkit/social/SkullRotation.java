package br.com.muxe.core.bukkit.social;

import java.util.List;
import org.bukkit.block.BlockFace;

public final class SkullRotation {

    private static final float STEP = 22.5f;
    private static final float HALF_TURN = 180f;

    private static final List<BlockFace> FACES = List.of(
        BlockFace.NORTH, BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST,
        BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST,
        BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST_SOUTH_WEST,
        BlockFace.WEST, BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST
    );

    private SkullRotation() {
    }

    public static BlockFace facing(float yaw) {
        return FACES.get(Math.floorMod(Math.round((yaw - HALF_TURN) / STEP), FACES.size()));
    }
}
