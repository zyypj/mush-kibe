package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentResult;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentType;
import br.com.muxe.core.common.placement.domain.HologramPlacement;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.Placement;
import br.com.muxe.core.common.placement.domain.Placements;
import br.com.muxe.core.common.placement.domain.RegisterPlacement;
import br.com.muxe.core.common.placement.domain.SocialHeadPlacement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.command.CommandSender;

public final class PlacementArgument<T extends Placement> implements ArgumentType<T> {

    private final Supplier<Placements> current;
    private final BiFunction<Placements, String, Optional<T>> find;
    private final Function<Placements, List<T>> all;
    private final String unknownKey;

    public PlacementArgument(Supplier<Placements> current, BiFunction<Placements, String, Optional<T>> find, Function<Placements, List<T>> all, String unknownKey) {
        this.current = current;
        this.find = find;
        this.all = all;
        this.unknownKey = unknownKey;
    }

    public static void registerAll(Commands commands, Supplier<Placements> current) {
        commands.argument(NpcPlacement.class, new PlacementArgument<>(current, Placements::npc, Placements::npcs, "npc-unknown"));
        commands.argument(HologramPlacement.class, new PlacementArgument<>(current, Placements::hologram, Placements::holograms, "hologram-unknown"));
        commands.argument(RegisterPlacement.class, new PlacementArgument<>(current, Placements::register, Placements::registers, "shop-register-unknown"));
        commands.argument(SocialHeadPlacement.class, new PlacementArgument<>(current, Placements::socialHead, Placements::socialHeads, "social-unknown"));
    }

    @Override
    public ArgumentResult<T> parse(CommandSender sender, String input) {
        String id = CommandText.id(input);
        return find.apply(current.get(), id)
            .map(ArgumentResult::of)
            .orElseGet(() -> ArgumentResult.rejected(unknownKey, "id", id));
    }

    @Override
    public Collection<String> suggest(CommandSender sender) {
        return all.apply(current.get()).stream().map(Placement::id).toList();
    }
}
