package br.com.muxe.core.bukkit.ultraplus;

import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.ultraplus.application.command.ClaimUltraPlusRewardHandler;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusStyle;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.bukkit.entity.Player;

public final class UltraPlusMenus {

    public static final String ONLY = "ultra-plus-only";
    public static final String LOCKED = "ultra-plus-locked";
    public static final String CLAIMED = "ultra-plus-reward-claimed";
    public static final String ALREADY_CLAIMED = "ultra-plus-reward-already";
    public static final String FAILED = "ultra-plus-reward-failed";

    private final Localized<UltraPlusSettings> settings;
    private final UltraPlusService service;
    private final MessageCatalog messages;
    private final Executor mainThread;
    private final Clock clock;

    public UltraPlusMenus(Localized<UltraPlusSettings> settings, UltraPlusService service, MessageCatalog messages, Executor mainThread, Clock clock) {
        this.settings = settings;
        this.service = service;
        this.messages = messages;
        this.mainThread = mainThread;
        this.clock = clock;
    }

    public void openMain(Player player, Optional<Consumer<Player>> back) {
        UltraPlusSettings current = settings.in(messages.language(player));
        YearMonth month = service.month(clock.instant());
        boolean claimed = service.of(player.getUniqueId()).claimed(month);
        List<UltraPlusPages.Shown> shown = UltraPlusPages.main(current, service.viewer(player), claimed, clock.instant());
        open(player, Menu.builder(current.main().title()).rows(current.main().rows()), shown, back, () -> openMain(player, back));
    }

    public void openColors(Player player, Optional<Consumer<Player>> back) {
        UltraPlusSettings current = settings.in(messages.language(player));
        open(player, Menu.builder(current.colors().title()).rows(current.colors().rows()), UltraPlusPages.colors(current, service.viewer(player)), back,
            () -> openColors(player, back));
    }

    public void openEmblems(Player player, Optional<Consumer<Player>> back) {
        UltraPlusSettings current = settings.in(messages.language(player));
        open(player, Menu.builder(current.emblems().title()).rows(current.emblems().rows()), UltraPlusPages.emblems(current, service.viewer(player)), back,
            () -> openEmblems(player, back));
    }

    private void open(Player player, Menu.Builder menu, List<UltraPlusPages.Shown> shown, Optional<Consumer<Player>> back, Runnable reopen) {
        for (UltraPlusPages.Shown entry : shown) {
            if (entry.click() == UltraPlusPages.Click.BACK && back.isEmpty()) {
                continue;
            }
            menu.button(entry.slot(), entry.icon(), context -> click(context.player(), entry, back, reopen));
        }
        menu.build().open(player);
    }

    private void click(Player player, UltraPlusPages.Shown entry, Optional<Consumer<Player>> back, Runnable reopen) {
        switch (entry.click()) {
            case NOTHING -> {
            }
            case BACK -> back.ifPresent(open -> open.accept(player));
            case COLORS -> openColors(player, Optional.of(viewer -> openMain(viewer, back)));
            case REWARD -> claim(player, reopen);
            case LEVEL -> choose(player, entry, style -> style.withTagLevel(Integer.parseInt(entry.value().orElseThrow())), reopen);
            case PLUS -> choose(player, entry, style -> style.withPlusColor(entry.value()), reopen);
            case EMBLEM -> choose(player, entry, style -> style.withEmblem(entry.value()), reopen);
        }
    }

    private void choose(Player player, UltraPlusPages.Shown entry, UnaryOperator<UltraPlusStyle> change, Runnable reopen) {
        if (!service.member(player.getUniqueId())) {
            player.sendMessage(messages.get(player, ONLY));
            return;
        }
        if (requiredMonths(player, entry) > service.months(player.getUniqueId())) {
            player.sendMessage(messages.get(player, LOCKED));
            return;
        }
        service.change(player, change.apply(service.of(player.getUniqueId()).style()));
        reopen.run();
    }

    private int requiredMonths(Player player, UltraPlusPages.Shown entry) {
        UltraPlusSettings current = settings.in(messages.language(player));
        String id = entry.value().orElse("");
        return switch (entry.click()) {
            case LEVEL -> current.main().levels().stream().filter(level -> level.id().equals(id)).findFirst().map(UltraPlusSettings.Option::months).orElse(0);
            case PLUS -> current.colors().options().stream().filter(color -> color.id().equals(id)).findFirst().map(UltraPlusSettings.Option::months).orElse(0);
            default -> 0;
        };
    }

    private void claim(Player player, Runnable reopen) {
        if (!service.member(player.getUniqueId())) {
            player.sendMessage(messages.get(player, ONLY));
            return;
        }
        YearMonth month = service.month(clock.instant());
        if (service.of(player.getUniqueId()).claimed(month)) {
            player.sendMessage(messages.get(player, ALREADY_CLAIMED));
            return;
        }
        String credits = Long.toString(service.settings().rewardCredits());
        service.claim(player, month).whenComplete((result, error) -> mainThread.execute(() -> {
            if (!player.isOnline()) {
                return;
            }
            if (error != null) {
                player.sendMessage(messages.get(player, FAILED));
            } else if (result instanceof Result.Failure<Void> failure) {
                player.sendMessage(messages.get(player, failure.problem().code().equals(ClaimUltraPlusRewardHandler.ALREADY_CLAIMED) ? ALREADY_CLAIMED : FAILED));
            } else {
                player.sendMessage(messages.get(player, CLAIMED, "credits", credits));
                reopen.run();
            }
        }));
    }
}
