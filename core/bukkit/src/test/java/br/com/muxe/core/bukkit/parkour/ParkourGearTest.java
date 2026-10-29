package br.com.muxe.core.bukkit.parkour;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

class ParkourGearTest {

    private final Map<Integer, ItemStack> slots = new HashMap<>();
    private boolean allowFlight;
    private boolean flying;
    private final Player player = player();

    @Test
    void startingSwapsTheItemsAndTurnsFlightOff() {
        slots.put(4, new ItemStack(Material.CHEST));
        allowFlight = true;
        flying = true;

        ParkourGear.equip(player, Map.of(3, new ItemStack(Material.RECORD_9), 4, new ItemStack(Material.GOLD_PLATE), 5, new ItemStack(Material.REDSTONE)));

        assertThat(slots.get(3).getType()).isEqualTo(Material.RECORD_9);
        assertThat(slots.get(4).getType()).isEqualTo(Material.GOLD_PLATE);
        assertThat(slots.get(5).getType()).isEqualTo(Material.REDSTONE);
        assertThat(allowFlight).isFalse();
        assertThat(flying).isFalse();
    }

    @Test
    void endingPutsBackWhatWasThereAndTheFlight() {
        slots.put(4, new ItemStack(Material.CHEST));
        slots.put(7, new ItemStack(Material.INK_SACK));
        allowFlight = true;
        flying = true;
        ParkourGear gear = ParkourGear.equip(player, Map.of(3, new ItemStack(Material.RECORD_9), 4, new ItemStack(Material.GOLD_PLATE)));
        slots.put(7, new ItemStack(Material.INK_SACK, 1, (short) 8));

        gear.restore(player);

        assertThat(slots.get(3)).isNull();
        assertThat(slots.get(4).getType()).isEqualTo(Material.CHEST);
        assertThat(slots.get(7).getDurability()).isEqualTo((short) 8);
        assertThat(allowFlight).isTrue();
        assertThat(flying).isTrue();
    }

    @Test
    void aPlayerWithoutFlightStaysWithoutFlight() {
        ParkourGear gear = ParkourGear.equip(player, Map.of(3, new ItemStack(Material.RECORD_9)));

        gear.restore(player);

        assertThat(allowFlight).isFalse();
        assertThat(flying).isFalse();
    }

    private Player player() {
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[] {PlayerInventory.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getItem" -> slots.get((Integer) args[0]);
                case "setItem" -> {
                    if (args[1] == null) {
                        slots.remove((Integer) args[0]);
                    } else {
                        slots.put((Integer) args[0], (ItemStack) args[1]);
                    }
                    yield null;
                }
                default -> null;
            });
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getInventory" -> inventory;
            case "getAllowFlight" -> allowFlight;
            case "isFlying" -> flying;
            case "setAllowFlight" -> {
                allowFlight = (Boolean) args[0];
                if (!allowFlight) {
                    flying = false;
                }
                yield null;
            }
            case "setFlying" -> {
                flying = (Boolean) args[0];
                yield null;
            }
            default -> null;
        });
    }
}
