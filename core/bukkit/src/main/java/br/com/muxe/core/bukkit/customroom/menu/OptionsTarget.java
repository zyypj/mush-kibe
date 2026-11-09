package br.com.muxe.core.bukkit.customroom.menu;

import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.schema.RoomLore;
import org.bukkit.entity.Player;

public interface OptionsTarget {

    RoomLore lore();

    RoomOptions options();

    void change(Player player, RoomOptions options);

    void back(Player player);

    String parentTitle();
}
