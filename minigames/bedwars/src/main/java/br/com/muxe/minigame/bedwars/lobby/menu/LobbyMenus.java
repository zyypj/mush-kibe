package br.com.muxe.minigame.bedwars.lobby.menu;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.prompt.ChatPrompts;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.api.arena.ArenaSchema;
import br.com.muxe.minigame.api.cosmetic.menu.MenuSlot;
import br.com.muxe.minigame.api.matchmaking.MatchConnector;
import br.com.muxe.minigame.api.matchmaking.RoomDirectory;
import br.com.muxe.minigame.api.network.ArenaInfo;
import br.com.muxe.minigame.bedwars.game.Levels;
import br.com.muxe.minigame.bedwars.lobby.LobbyLines;
import br.com.muxe.minigame.bedwars.lobby.format.LevelBadges;
import br.com.muxe.minigame.bedwars.lobby.top.TagColors;
import br.com.muxe.minigame.bedwars.lobby.top.TopData;
import br.com.muxe.minigame.bedwars.stats.application.command.ToggleFavouriteMapCommand;
import br.com.muxe.minigame.bedwars.stats.application.query.FindFavouriteMapsQuery;
import br.com.muxe.minigame.bedwars.stats.application.query.FindPlayerStatsQuery;
import br.com.muxe.minigame.bedwars.stats.domain.PlayerStats;
import br.com.muxe.minigame.bedwars.stats.domain.StatsPeriod;
import br.com.muxe.minigame.bedwars.stats.domain.TopCategory;
import br.com.muxe.minigame.bedwars.stats.domain.TopEntry;
import br.com.muxe.minigame.engine.mode.ModeNames;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class LobbyMenus {

    public static final String MAP_PERMISSION = "muxe.bedwars.map-select";
    public static final Duration SEARCH_TIMEOUT = Duration.ofSeconds(60);

    private final MuxeCore core;
    private final Localized<LobbyMenuSettings> settings;
    private final ArenaSchema schema;
    private final ModeNames modeNames;
    private final MatchConnector connector;
    private final TopData tops;
    private final TagColors colors;
    private final LevelBadges badges;
    private final MessageCatalog messages;
    private final ChatPrompts prompts;

    public LobbyMenus(Plugin plugin, MuxeCore core, Localized<LobbyMenuSettings> settings, ArenaSchema schema, ModeNames modeNames, MatchConnector connector,
                      TopData tops, TagColors colors, LevelBadges badges, MessageCatalog messages) {
        this.core = core;
        this.settings = settings;
        this.schema = schema;
        this.modeNames = modeNames;
        this.connector = connector;
        this.tops = tops;
        this.colors = colors;
        this.badges = badges;
        this.messages = messages;
        this.prompts = ChatPrompts.start(plugin, core.clock());
    }

    public String modeName(Language language, String mode) {
        return schema.modes().contains(mode) ? modeNames.in(language, mode) : mode;
    }

    public List<String> topModeOrder() {
        return List.copyOf(settings.standard().topModes().entries().keySet());
    }

    public String modeLabel(Language language, String mode) {
        if (schema.modes().contains(mode)) {
            return modeNames.in(language, mode);
        }
        return mode.isEmpty() ? mode : Character.toUpperCase(mode.charAt(0)) + mode.substring(1);
    }

    public String modeTitle(Language language, String mode) {
        LobbyMenuSettings.TopModeEntry entry = settings.in(language).topModes().entries().get(mode);
        return entry == null ? modeName(language, mode) : entry.title();
    }

    public void openMode(Player player, String mode) {
        Language language = messages.language(player);
        LobbyMenuSettings settings = this.settings.in(language);
        LobbyMenuSettings.Mode menu = settings.mode();
        String name = modeName(language, mode);
        RoomDirectory directory = connector.directory();
        Menu.Builder builder = Menu.builder(menu.title().replace("{mode}", name)).rows(menu.rows());
        ItemBuilder play = menu.play().item().replacing("mode", name, "players", String.valueOf(directory.playersIn(mode)));
        builder.button(menu.play().slot(), play.lore(expandTop(play.lore(), mode, settings)), context -> {
            context.close();
            connector.join(context.player(), mode, Optional.empty());
        });
        builder.button(menu.map().slot(), menu.map().item(), context -> {
            if (!context.player().hasPermission(MAP_PERMISSION)) {
                context.player().sendMessage(messages.get(language, "map-vip-only"));
                return;
            }
            openMaps(context.player(), mode, 0, Optional.empty());
        });
        builder.button(menu.training().slot(), menu.training().item(), context -> context.player().sendMessage(messages.get(language, "coming-soon")));
        if (menu.backModes().contains(mode)) {
            builder.button(menu.back().slot(), menu.back().item(), context -> openDuels(context.player()));
        }
        builder.build().open(player);
    }

    public void openDuels(Player player) {
        Language language = messages.language(player);
        LobbyMenuSettings.Duels menu = settings.in(language).duels();
        Menu.Builder builder = Menu.builder(menu.title()).rows(menu.rows());
        for (Map.Entry<String, LobbyMenuSettings.DuelEntry> entry : menu.entries().entrySet()) {
            String mode = entry.getKey();
            ItemBuilder icon = menu.item().amount(entry.getValue().amount())
                .replacing("mode", modeName(language, mode), "players", String.valueOf(connector.directory().playersIn(mode)));
            builder.button(entry.getValue().slot(), icon, context -> openMode(context.player(), mode));
        }
        builder.build().open(player);
    }

    public void openMaps(Player player, String mode, int page, Optional<String> search) {
        core.queries().ask(new FindFavouriteMapsQuery(player.getUniqueId())).whenCompleteAsync((favourites, error) -> {
            if (player.isOnline()) {
                showMaps(player, mode, page, search, error == null ? favourites : Set.of());
            }
        }, core.mainThread());
    }

    private void showMaps(Player player, String mode, int page, Optional<String> search, Set<String> favourites) {
        Language language = messages.language(player);
        LobbyMenuSettings.Maps menu = settings.in(language).maps();
        List<ArenaInfo> arenas = MapList.order(connector.directory().arenas(mode), favourites, search);
        if (search.isPresent() && arenas.isEmpty()) {
            player.sendMessage(messages.get(language, "map-search-none", "search", search.get()));
            showMaps(player, mode, 0, Optional.empty(), favourites);
            return;
        }
        int perPage = Math.max(1, menu.slots().size());
        int pages = Math.max(1, (arenas.size() + perPage - 1) / perPage);
        int current = Math.max(0, Math.min(page, pages - 1));
        String name = modeName(language, mode);
        Menu.Builder builder = Menu.builder(menu.title().replace("{mode}", name).replace("{page}", String.valueOf(current + 1)).replace("{pages}", String.valueOf(pages)))
            .rows(menu.rows());
        List<ArenaInfo> shown = arenas.subList(current * perPage, Math.min(arenas.size(), (current + 1) * perPage));
        for (int index = 0; index < shown.size(); index++) {
            ArenaInfo arena = shown.get(index);
            RoomDirectory.RoomCount rooms = connector.directory().rooms(mode, arena.name());
            ItemBuilder icon = (favourites.contains(arena.name()) ? menu.favourite() : menu.other())
                .replacing("map", arena.displayName(), "available", String.valueOf(rooms.available()), "total", String.valueOf(rooms.total()));
            builder.button(menu.slots().get(index), icon, context -> {
                if (context.isRightClick()) {
                    core.commands().dispatch(new ToggleFavouriteMapCommand(player.getUniqueId(), arena.name()))
                        .whenCompleteAsync((result, error) -> openMaps(player, mode, current, search), core.mainThread());
                    return;
                }
                context.close();
                connector.join(context.player(), mode, Optional.of(arena.name()));
            });
        }
        builder.button(menu.back().slot(), menu.back().item().replacing("mode", name), context -> openMode(context.player(), mode));
        builder.button(menu.random().slot(), menu.random().item(), context -> {
            List<ArenaInfo> liked = arenas.stream().filter(arena -> favourites.contains(arena.name())).toList();
            if (liked.isEmpty()) {
                context.player().sendMessage(messages.get(language, "map-no-favourites"));
                return;
            }
            context.close();
            connector.join(context.player(), mode, Optional.of(liked.get(ThreadLocalRandom.current().nextInt(liked.size())).name()));
        });
        builder.button(menu.search().slot(), menu.search().item(), context -> {
            context.close();
            context.player().sendMessage(messages.get(language, "map-search"));
            prompts.ask(context.player(), SEARCH_TIMEOUT, text -> openMaps(player, mode, 0, Optional.of(text)), () -> openMaps(player, mode, 0, Optional.empty()), () -> {
            });
        });
        if (current > 0) {
            builder.button(menu.previous().slot(), menu.previous().item(), context -> showMaps(context.player(), mode, current - 1, search, favourites));
        }
        if (current < pages - 1) {
            builder.button(menu.next().slot(), menu.next().item(), context -> showMaps(context.player(), mode, current + 1, search, favourites));
        }
        builder.build().open(player);
    }

    public void openTopModes(Player player, String current, Consumer<String> chosen) {
        LobbyMenuSettings.TopModes menu = settings.in(messages.language(player)).topModes();
        Menu.Builder builder = Menu.builder(menu.title()).rows(menu.rows());
        for (Map.Entry<String, LobbyMenuSettings.TopModeEntry> entry : menu.entries().entrySet()) {
            String mode = entry.getKey();
            boolean selected = mode.equals(current);
            ItemBuilder icon = entry.getValue().item().lore(selected ? menu.selectedLore() : menu.otherLore()).glow(selected);
            builder.button(entry.getValue().slot(), icon, context -> {
                context.close();
                if (!selected) {
                    chosen.accept(mode);
                }
            });
        }
        builder.build().open(player);
    }

    public void openStats(Player player) {
        core.queries().ask(new FindPlayerStatsQuery(player.getUniqueId())).whenCompleteAsync((stats, error) -> {
            if (player.isOnline()) {
                showStats(player, error == null ? stats : PlayerStats.EMPTY);
            }
        }, core.mainThread());
    }

    private void showStats(Player player, PlayerStats stats) {
        LobbyMenuSettings.Stats menu = settings.in(messages.language(player)).stats();
        Menu.Builder builder = Menu.builder(menu.title()).rows(menu.rows());
        Levels.Progress progress = Levels.progress(Math.max(0, stats.general().xp()));
        ItemBuilder general = menu.general().item();
        builder.button(menu.general().slot(), general.lore(general.lore().stream()
            .map(line -> LobbyLines.counters(line, stats.general()).replace("{level}", badges.of(progress.level()))).toList()));
        for (Map.Entry<String, MenuSlot> entry : menu.entries().entrySet()) {
            ItemBuilder icon = entry.getValue().item();
            builder.button(entry.getValue().slot(), icon.lore(menu.lore().stream().map(line -> LobbyLines.counters(line, stats.of(entry.getKey()))).toList()));
        }
        builder.build().open(player);
    }

    private List<String> expandTop(List<String> lore, String mode, LobbyMenuSettings settings) {
        List<TopEntry> top = tops.get(TopCategory.WINSTREAK, mode, StatsPeriod.DAILY);
        List<String> expanded = new ArrayList<>();
        for (String line : lore) {
            if (!line.contains("{top}")) {
                expanded.add(line);
                continue;
            }
            List<String> formats = settings.mode().topLines();
            int shown = Math.min(formats.size(), top.size());
            if (shown == 0) {
                expanded.add(settings.mode().topEmpty());
            }
            for (int index = 0; index < shown; index++) {
                TopEntry entry = top.get(index);
                expanded.add(formats.get(index).replace("{name}", colors.of(entry.playerId()) + entry.name()).replace("{value}", String.valueOf(entry.value())));
            }
        }
        return expanded;
    }
}
