package br.com.muxe.core.bukkit.customroom.menu;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.customroom.CustomRoomSettings;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.prompt.AnvilEdits;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.customroom.application.command.DeletePresetCommand;
import br.com.muxe.core.common.customroom.application.command.SavePresetCommand;
import br.com.muxe.core.common.customroom.application.query.FindPresetQuery;
import br.com.muxe.core.common.customroom.application.query.ListPresetsQuery;
import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.domain.RoomPreset;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.result.Result;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public final class PresetMenu {

    private final MuxeCore core;
    private final Localized<CustomRoomSettings> localized;
    private final AnvilEdits anvils;

    public PresetMenu(MuxeCore core, CustomRoomSettings settings, AnvilEdits anvils) {
        this(core, Localized.same(settings), anvils);
    }

    public PresetMenu(MuxeCore core, Localized<CustomRoomSettings> settings, AnvilEdits anvils) {
        this.core = core;
        this.localized = settings;
        this.anvils = anvils;
    }

    public void open(Player player, Target target) {
        core.queries().ask(new ListPresetsQuery(player.getUniqueId(), target.game())).whenCompleteAsync((presets, error) -> {
            if (!player.isOnline()) {
                return;
            }
            show(player, target, error == null ? presets : List.of());
        }, core.mainThread());
    }

    private void show(Player player, Target target, List<RoomPreset> presets) {
        CustomRoomSettings settings = settingsOf(player);
        ConfigurationSection section = settings.section("presets-menu");
        Menu.Builder menu = Menu.builder(Colors.translate(section.getString("title", "Predefinições"))).rows(section.getInt("rows", 4));
        List<Integer> slots = section.getIntegerList("slots");
        if (presets.isEmpty()) {
            menu.button(settings.slot("presets-menu.empty"), settings.item("presets-menu.empty"));
        }
        for (int index = 0; index < presets.size() && index < slots.size(); index++) {
            RoomPreset preset = presets.get(index);
            ItemBuilder icon = settings.item("presets-menu.preset").replacing("name", preset.name(), "mode", target.modeName().apply(preset.mode()), "code", preset.code());
            menu.button(slots.get(index), icon, context -> clicked(context.player(), target, preset, context.type()));
        }
        menu.button(settings.slot("presets-menu.save"), settings.item("presets-menu.save"), context -> askName(context.player(), target));
        menu.button(section.getInt("back-slot", 31), OptionMenus.backButton(settings.texts(), target.backTitle()), context -> target.back().accept(context.player()));
        menu.button(settings.slot("presets-menu.code"), settings.item("presets-menu.code"), context -> askCode(context.player(), target));
        menu.build().open(player);
    }

    private void clicked(Player player, Target target, RoomPreset preset, ClickType type) {
        switch (type) {
            case DROP, CONTROL_DROP -> core.commands().dispatch(new DeletePresetCommand(player.getUniqueId(), preset.id())).whenCompleteAsync((deleted, error) -> {
                if (Boolean.TRUE.equals(deleted)) {
                    player.sendMessage(core.messages().get(player, "custom-room-preset-deleted", "name", preset.name()));
                }
                open(player, target);
            }, core.mainThread());
            case RIGHT, SHIFT_RIGHT -> player.sendMessage(core.messages().get(player, "custom-room-preset-share", "name", preset.name(), "code", preset.code()));
            default -> load(player, target, preset);
        }
    }

    private void load(Player player, Target target, RoomPreset preset) {
        if (!preset.game().equals(target.game())) {
            player.sendMessage(core.messages().get(player, "custom-room-preset-other-game"));
            return;
        }
        target.loaded().accept(preset);
        player.sendMessage(core.messages().get(player, "custom-room-preset-loaded", "name", preset.name()));
        target.back().accept(player);
    }

    private void askName(Player player, Target target) {
        CustomRoomSettings settings = settingsOf(player);
        String label = settings.text("presets-menu.name-label", "Nome");
        anvils.edit(player, " ", typed -> settings.texts().get("anvil-result", "label", label, "value", typed), typed -> {
            Snapshot snapshot = target.snapshot().get();
            core.commands().dispatch(new SavePresetCommand(player.getUniqueId(), target.game(), typed, snapshot.mode(), snapshot.maps(), snapshot.options()))
                .whenCompleteAsync((result, error) -> {
                    if (error == null && result instanceof Result.Success<RoomPreset> saved) {
                        player.sendMessage(core.messages().get(player, "custom-room-preset-saved", "name", saved.value().name(), "code", saved.value().code()));
                    } else {
                        player.sendMessage(core.messages().get(player, "custom-room-preset-failed"));
                    }
                    open(player, target);
                }, core.mainThread());
        }, () -> open(player, target));
    }

    private void askCode(Player player, Target target) {
        CustomRoomSettings settings = settingsOf(player);
        String label = settings.text("presets-menu.code-label", "Código");
        anvils.edit(player, " ", typed -> settings.texts().get("anvil-result", "label", label, "value", typed), typed ->
            core.queries().ask(new FindPresetQuery(typed)).whenCompleteAsync((found, error) -> {
                if (error != null || found == null || found.isEmpty()) {
                    player.sendMessage(core.messages().get(player, "custom-room-preset-not-found"));
                    open(player, target);
                    return;
                }
                load(player, target, found.get());
            }, core.mainThread()), () -> open(player, target));
    }

    private CustomRoomSettings settingsOf(Player player) {
        return localized.in(core.messages().language(player));
    }

    public record Snapshot(String mode, List<String> maps, RoomOptions options) {
    }

    public record Target(String game, Function<String, String> modeName, Supplier<Snapshot> snapshot, Consumer<RoomPreset> loaded,
                         Consumer<Player> back, String backTitle) {
    }
}
