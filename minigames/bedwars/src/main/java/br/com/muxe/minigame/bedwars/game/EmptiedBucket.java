package br.com.muxe.minigame.bedwars.game;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class EmptiedBucket {

    private EmptiedBucket() {
    }

    public static ItemStack left(ItemStack inSlot) {
        if (inSlot == null || inSlot.getType() != Material.BUCKET) {
            return inSlot;
        }
        if (inSlot.getAmount() <= 1) {
            return null;
        }
        ItemStack fewer = inSlot.clone();
        fewer.setAmount(inSlot.getAmount() - 1);
        return fewer;
    }
}
