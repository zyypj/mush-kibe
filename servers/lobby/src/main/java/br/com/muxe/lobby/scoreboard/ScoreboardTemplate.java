package br.com.muxe.lobby.scoreboard;

import br.com.muxe.lobby.profile.PlayerInfo;
import java.util.List;
import java.util.Objects;

public record ScoreboardTemplate(String title, List<String> lines) {

    public ScoreboardTemplate {
        Objects.requireNonNull(title, "title");
        lines = List.copyOf(lines);
    }

    public List<String> render(PlayerInfo info, String lobby, int players) {
        return lines.stream()
            .map(line -> line
                .replace("{tag}", info.tag())
                .replace("{credits}", Long.toString(info.credits()))
                .replace("{lobby}", lobby)
                .replace("{players}", Integer.toString(players)))
            .toList();
    }
}
