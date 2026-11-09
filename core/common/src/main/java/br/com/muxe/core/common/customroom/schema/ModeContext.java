package br.com.muxe.core.common.customroom.schema;

public record ModeContext(String mode, int teamSize, int maxPlayers, int teams) {

    public ModeContext {
        teamSize = Math.max(1, teamSize);
        maxPlayers = Math.max(1, maxPlayers);
        teams = Math.max(1, teams);
    }

    public int resolve(String token, int fallback) {
        if (token == null || token.isBlank()) {
            return fallback;
        }
        return switch (token.trim()) {
            case "mode-team-size" -> teamSize;
            case "mode-players" -> maxPlayers;
            case "mode-teams" -> teams;
            default -> {
                try {
                    yield (int) Math.round(Double.parseDouble(token.trim()));
                } catch (NumberFormatException exception) {
                    yield fallback;
                }
            }
        };
    }

    public double resolveDecimal(String token, double fallback) {
        if (token == null || token.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(token.trim());
        } catch (NumberFormatException exception) {
            return resolve(token, (int) fallback);
        }
    }
}
