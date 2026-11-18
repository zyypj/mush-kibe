package br.com.muxe.core.bukkit.ultraplus;

import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusGrant;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusStyle;
import java.util.Objects;
import java.util.Optional;

public record UltraPlusLook(UltraPlusSettings.Option level, UltraPlusSettings.Option plus, Optional<UltraPlusSettings.Emblem> emblem) {

    public UltraPlusLook {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(plus, "plus");
        Objects.requireNonNull(emblem, "emblem");
    }

    public static UltraPlusLook of(UltraPlusSettings settings, UltraPlusStyle style, int months) {
        UltraPlusSettings.Option chosenLevel = settings.main().level(style.tagLevel());
        UltraPlusSettings.Option level = chosenLevel.months() <= months ? chosenLevel : settings.main().levels().getFirst();
        UltraPlusSettings.Option chosenPlus = settings.colors().option(style.plusColor());
        UltraPlusSettings.Option plus = chosenPlus.months() <= months ? chosenPlus : settings.colors().options().getFirst();
        return new UltraPlusLook(level, plus, settings.emblems().emblem(style.emblem()));
    }

    public String emblemText() {
        return emblem.map(UltraPlusSettings.Emblem::render).orElse("");
    }

    public RankDisplay apply(UltraPlusSettings settings, RankDisplay display) {
        Optional<Tag> tag = display.tag().map(shown -> shown.id().equals(UltraPlusGrant.GROUP)
            ? new Tag(shown.id(), settings.tagName(plus), level.color(), shown.weight(), Optional.of(settings.prefix(level, plus)), shown.decorative())
            : shown);
        return display.withTag(tag).withEmblem(emblemText());
    }
}
