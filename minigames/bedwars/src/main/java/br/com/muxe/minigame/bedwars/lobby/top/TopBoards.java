package br.com.muxe.minigame.bedwars.lobby.top;

import br.com.muxe.core.bukkit.hologram.HologramPixel;
import br.com.muxe.core.bukkit.hologram.LineAim;
import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import br.com.muxe.core.bukkit.hologram.TextHolograms;
import br.com.muxe.core.bukkit.nms.EntityUse;
import br.com.muxe.core.bukkit.nms.PacketInterceptor;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.bedwars.lobby.format.LevelBadges;
import br.com.muxe.minigame.bedwars.stats.domain.PlayerStats;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class TopBoards implements Listener {

    public static final long JOIN_DELAY_TICKS = 20L;

    private final Plugin plugin;
    private final Localized<List<TopBoardSettings>> boards;
    private final Localized<TopTexts> localizedTexts;
    private final TopTexts texts;
    private final TopData data;
    private final TagColors colors;
    private final LevelBadges badges;
    private final BiFunction<Language, String, String> modeTitle;
    private final ModeChooser chooser;
    private final Localized<Optional<TopSelectorSettings>> selectors;
    private final List<String> modeOrder;
    private final BiFunction<Language, String, String> modeName;
    private final LanguageLookup languages;
    private final TextHolograms holograms;
    private final Map<UUID, Map<String, Shown>> shown = new ConcurrentHashMap<>();
    private final Map<UUID, SelectorShown> shownSelectors = new ConcurrentHashMap<>();
    private final Map<UUID, String> modes = new ConcurrentHashMap<>();
    private Optional<PacketInterceptor> interceptor = Optional.empty();
    private boolean refreshQueued;

    public TopBoards(Plugin plugin, Localized<List<TopBoardSettings>> boards, Localized<TopTexts> texts, TopData data, TagColors colors, LevelBadges badges,
                     BiFunction<Language, String, String> modeTitle, ModeChooser chooser, Localized<Optional<TopSelectorSettings>> selectors,
                     List<String> modeOrder, BiFunction<Language, String, String> modeName, LanguageLookup languages) {
        this.plugin = plugin;
        this.boards = boards.map(List::copyOf);
        this.localizedTexts = texts;
        this.texts = texts.standard();
        this.data = data;
        this.colors = colors;
        this.badges = badges;
        this.modeTitle = modeTitle;
        this.chooser = chooser;
        this.selectors = selectors;
        this.modeOrder = List.copyOf(modeOrder);
        this.modeName = modeName;
        this.languages = languages;
        this.holograms = TextHolograms.standard(selectors.standard().map(TopSelectorSettings::listSpacing).orElse(TextHolograms.DEFAULT_LINE_SPACING));
    }

    public void refresh(Player player) {
        if (shown.containsKey(player.getUniqueId()) || shownSelectors.containsKey(player.getUniqueId())) {
            show(player);
        }
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        interceptor = PacketInterceptor.start(plugin, this::onUse);
        data.onChange(this::queueRefresh);
        colors.onChange(this::queueRefresh);
        plugin.getServer().getOnlinePlayers().forEach(this::show);
        long period = 20L * texts.refreshSeconds();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshAll, period, period);
        plugin.getServer().getScheduler().runTaskTimer(plugin, colors::forgetAll, period * 10, period * 10);
    }

    public void stop() {
        interceptor.ifPresent(PacketInterceptor::stop);
        shown.values().forEach(views -> views.values().forEach(view -> view.hologram().remove()));
        shown.clear();
        shownSelectors.values().forEach(SelectorShown::remove);
        shownSelectors.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> show(player), JOIN_DELAY_TICKS);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        hide(player);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> show(player), JOIN_DELAY_TICKS);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        shown.remove(id);
        shownSelectors.remove(id);
        modes.remove(id);
    }

    private void show(Player player) {
        if (!player.isOnline()) {
            return;
        }
        hide(player);
        String mode = mode(player);
        Language language = languages.of(player.getUniqueId());
        Map<String, Shown> views = new ConcurrentHashMap<>();
        for (TopBoardSettings board : boards.in(language)) {
            Location top = top(board);
            if (top.getWorld() == null || !top.getWorld().equals(player.getWorld())) {
                continue;
            }
            TopState state = new TopState();
            if (board.modes()) {
                state.mode(mode);
            }
            views.put(board.id(), new Shown(board, state, language, holograms.show(player, top, lines(board, state, language), TopView.gaps(texts.spacing()))));
        }
        shown.put(player.getUniqueId(), views);
        selectors.in(language).ifPresent(settings -> {
            Location header = settings.spot().above(texts.labelAboveStand()).toLocation(plugin.getServer());
            if (header.getWorld() != null && header.getWorld().equals(player.getWorld())) {
                shownSelectors.put(player.getUniqueId(), new SelectorShown(
                    holograms.show(player, header, List.of(settings.header())),
                    holograms.show(player, header.clone().subtract(0, settings.listOffset(), 0), selectorModes(settings, mode, language)),
                    holograms.show(player, header.clone().subtract(0, settings.footerOffset(), 0), List.of(settings.footer()))
                ));
            }
        });
    }

    private void hide(Player player) {
        Map<String, Shown> views = shown.remove(player.getUniqueId());
        if (views != null) {
            views.values().forEach(view -> view.hologram().remove());
        }
        SelectorShown shownSelector = shownSelectors.remove(player.getUniqueId());
        if (shownSelector != null) {
            shownSelector.remove();
        }
    }

    private String mode(Player player) {
        return modes.getOrDefault(player.getUniqueId(), PlayerStats.GENERAL);
    }

    private List<String> selectorModes(TopSelectorSettings settings, String mode, Language language) {
        return TopSelectorView.modes(settings, modeOrder, mode, id -> modeName.apply(language, id));
    }

    private void queueRefresh() {
        if (refreshQueued) {
            return;
        }
        refreshQueued = true;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            refreshQueued = false;
            refreshAll();
        });
    }

    private void refreshAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Map<String, Shown> views = shown.get(player.getUniqueId());
            if (views != null) {
                views.keySet().forEach(id -> refresh(player, id));
            }
        }
    }

    private void refresh(Player player, String boardId) {
        Map<String, Shown> views = shown.get(player.getUniqueId());
        Shown view = views == null ? null : views.get(boardId);
        if (view == null) {
            return;
        }
        Location top = top(view.board());
        List<HologramPixel> pixels = holograms.layout(top, lines(view.board(), view.state(), view.language()), TopView.gaps(texts.spacing()));
        if (view.hologram().rename(pixels.stream().map(HologramPixel::name).toList())) {
            return;
        }
        view.hologram().remove();
        views.put(boardId, new Shown(view.board(), view.state(), view.language(),
            holograms.show(player, top, lines(view.board(), view.state(), view.language()), TopView.gaps(texts.spacing()))));
    }

    private void refreshSelector(Player player) {
        SelectorShown shownSelector = shownSelectors.get(player.getUniqueId());
        Language language = languages.of(player.getUniqueId());
        Optional<TopSelectorSettings> selector = selectors.in(language);
        if (shownSelector == null || selector.isEmpty()) {
            return;
        }
        TopSelectorSettings settings = selector.get();
        Location list = settings.spot().above(texts.labelAboveStand() - settings.listOffset()).toLocation(plugin.getServer());
        List<String> lines = selectorModes(settings, mode(player), language);
        List<String> names = holograms.layout(list, lines).stream().map(HologramPixel::name).toList();
        if (!shownSelector.list().rename(names)) {
            shownSelector.list().remove();
            shownSelectors.put(player.getUniqueId(), new SelectorShown(shownSelector.header(), holograms.show(player, list, lines), shownSelector.footer()));
        }
    }

    private List<String> lines(TopBoardSettings board, TopState state, Language language) {
        String mode = board.modes() ? state.mode() : PlayerStats.GENERAL;
        return TopView.lines(board, localizedTexts.in(language), state, data.get(board.category(), mode, TopView.period(board, state)), colors::of, badges,
            id -> modeTitle.apply(language, id));
    }

    private Location top(TopBoardSettings board) {
        return board.spot().above(texts.labelAboveStand()).toLocation(plugin.getServer());
    }

    private void onUse(Player player, int entityId, EntityUse use) {
        if (use == EntityUse.INTERACT_AT) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            Map<String, Shown> views = shown.get(player.getUniqueId());
            if (views != null) {
                for (Shown view : views.values()) {
                    if (view.hologram().owns(entityId)) {
                        click(player);
                        clicked(player, view);
                        return;
                    }
                }
            }
            SelectorShown shownSelector = shownSelectors.get(player.getUniqueId());
            if (shownSelector != null && shownSelector.owns(entityId)) {
                click(player);
                chooser.choose(player, mode(player), mode -> choose(player, mode));
            }
        });
    }

    private void click(Player player) {
        texts.clickSound().ifPresent(sound -> player.playSound(player.getLocation(), sound, 1f, 1f));
    }

    private void choose(Player player, String mode) {
        modes.put(player.getUniqueId(), mode);
        Map<String, Shown> views = shown.get(player.getUniqueId());
        if (views != null) {
            views.values().stream().filter(view -> view.board().modes()).forEach(view -> {
                view.state().mode(mode);
                refresh(player, view.board().id());
            });
        }
        refreshSelector(player);
    }

    private void clicked(Player player, Shown view) {
        TopBoardSettings board = view.board();
        Location top = top(board);
        double boundary = top.getY() - TopView.clickBoundary(texts.spacing());
        if (LineAim.heightAt(player.getEyeLocation(), top.getX(), top.getZ()) >= boundary) {
            view.state().nextPage();
        } else {
            view.state().nextPeriod(board.periods().size());
        }
        refresh(player, board.id());
    }

    @FunctionalInterface
    public interface ModeChooser {

        void choose(Player player, String current, Consumer<String> chosen);
    }

    private record Shown(TopBoardSettings board, TopState state, Language language, SpawnedHologram hologram) {
    }

    private record SelectorShown(SpawnedHologram header, SpawnedHologram list, SpawnedHologram footer) {

        boolean owns(int entityId) {
            return header.owns(entityId) || list.owns(entityId) || footer.owns(entityId);
        }

        void remove() {
            header.remove();
            list.remove();
            footer.remove();
        }
    }
}
