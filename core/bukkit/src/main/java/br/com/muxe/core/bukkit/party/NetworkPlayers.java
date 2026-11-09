package br.com.muxe.core.bukkit.party;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;

public final class NetworkPlayers {

    public static final String SUGGESTIONS = "network-players";

    private final Supplier<Collection<String>> local;
    private volatile List<String> network = List.of();

    public NetworkPlayers(Supplier<Collection<String>> local) {
        this.local = local;
    }

    public void update(List<String> names) {
        network = List.copyOf(names);
    }

    public List<String> names() {
        TreeSet<String> all = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        all.addAll(network);
        all.addAll(local.get());
        return List.copyOf(all);
    }

    public List<String> namesExcept(String self) {
        return names().stream().filter(name -> !name.equalsIgnoreCase(self)).toList();
    }
}
