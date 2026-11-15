package br.com.muxe.minigame.bedwars.mission;

import br.com.muxe.core.common.mission.domain.MissionGoal;
import java.util.Arrays;
import java.util.Optional;

public enum BedWarsGoal {
    KILL_STREAK(true),
    INVISIBLE_BED(false),
    END_STONE_DEFENSE(false),
    CLEAN_WIN(false),
    BEDBUG_KILLS(false),
    EMERALDS_IN_A_LIFE(true),
    FIREBALL_DAMAGE(false),
    FINAL_KILLS(false),
    EXPLOSIVE_BLOCKS(false),
    FAST_WIN(false),
    CLAY_BOUGHT(false),
    BRIDGE_EGGS(false),
    FEW_BLOCKS_WIN(false),
    DIAMONDS(false),
    BOW_DAMAGE(false),
    SHARPNESS_TWO(false),
    KILLS(false),
    MILK_CLEANSE(false);

    private final MissionGoal goal;

    BedWarsGoal(boolean best) {
        this.goal = new MissionGoal(name(), best);
    }

    public MissionGoal goal() {
        return goal;
    }

    public static Optional<MissionGoal> find(String name) {
        return Arrays.stream(values()).filter(value -> value.name().equals(name)).map(BedWarsGoal::goal).findFirst();
    }
}
