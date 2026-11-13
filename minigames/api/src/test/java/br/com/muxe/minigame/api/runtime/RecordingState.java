package br.com.muxe.minigame.api.runtime;

import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class RecordingState implements MatchState {

    public final List<String> calls = new ArrayList<>();
    public final List<Death> deaths = new ArrayList<>();
    public Optional<TeamColor> winner = Optional.empty();

    @Override
    public void arrived(UUID player) {
        calls.add("arrived");
    }

    @Override
    public void starting() {
        calls.add("starting");
    }

    @Override
    public void spawned(Participant player) {
        calls.add("spawned " + player.name());
    }

    @Override
    public void started() {
        calls.add("started");
    }

    @Override
    public void died(Death death) {
        deaths.add(death);
    }

    @Override
    public void ending() {
        calls.add("ending");
    }

    @Override
    public void ended(Optional<TeamColor> winner) {
        calls.add("ended");
        this.winner = winner;
    }

    @Override
    public void optionsChanged() {
        calls.add("options changed");
    }

    @Override
    public void closed() {
        calls.add("closed");
    }

    @Override
    public Map<String, String> placeholders(Optional<Participant> viewer) {
        return Map.of("points", "7");
    }
}
