package br.com.muxe.minigame.engine.setup.session;

import br.com.muxe.core.bukkit.text.ChatButton;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.setup.SetupChecklist;
import br.com.muxe.minigame.engine.setup.SetupChecklist.Item;
import br.com.muxe.minigame.engine.setup.SetupStep;
import br.com.muxe.minigame.engine.setup.StepKind;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SetupHelp {

    public static final String COMMAND = "/" + SetupCommand.NAME;

    private final MessageCatalog messages;

    public SetupHelp(MessageCatalog messages) {
        this.messages = messages;
    }

    public String label(SetupStep step, TeamColor team) {
        if (team == null) {
            return step.label();
        }
        return messages.get("setup-label-team", "label", step.label(), "team", Colors.translate(team.colored()));
    }

    public static String command(String action, SetupStep step, TeamColor team) {
        return COMMAND + " " + action + " " + step.id() + (team == null ? "" : " " + team.id());
    }

    public List<List<ChatButton>> lines(ArenaDraft draft, SetupChecklist checklist) {
        List<List<ChatButton>> lines = new ArrayList<>();
        for (String header : messages.lines("setup-help-header", "arena", draft.name(), "display", draft.displayName(), "mode", draft.mode())) {
            lines.add(List.of(ChatButton.text(header)));
        }
        lines.add(teams(draft, checklist));
        for (Item item : checklist.items()) {
            lines.add(item(item));
        }
        lines.add(List.of(ChatButton.text(messages.get("setup-help-footer"))));
        lines.add(List.of(
            ChatButton.run(messages.get("setup-help-save"), COMMAND + " salvar", messages.get("setup-help-save-hover")),
            ChatButton.suggest(messages.get("setup-help-rename"), COMMAND + " nome ", messages.get("setup-help-rename-hover")),
            ChatButton.run(messages.get("setup-help-leave"), COMMAND + " sair", messages.get("setup-help-leave-hover"))
        ));
        return lines;
    }

    public String actionBar(SetupChecklist checklist) {
        if (checklist.isReady()) {
            return messages.get("setup-actionbar-ready");
        }
        List<String> missing = new ArrayList<>();
        if (!checklist.teamsDone()) {
            missing.add(messages.get("setup-help-teams", "mark", "", "count", String.valueOf(checklist.teamCount()), "total", String.valueOf(checklist.teamRule().count()), "teams", "").trim());
        }
        checklist.missing().forEach(item -> missing.add(label(item.step(), item.team())));
        String more = missing.size() > 1 ? messages.get("setup-actionbar-more", "count", String.valueOf(missing.size() - 1)) : "";
        return messages.get("setup-actionbar-missing", "next", missing.getFirst(), "more", more);
    }

    private List<ChatButton> teams(ArenaDraft draft, SetupChecklist checklist) {
        List<ChatButton> line = new ArrayList<>();
        String names = draft.teams().isEmpty()
            ? messages.get("setup-help-teams-none")
            : draft.teams().stream().map(team -> Colors.translate(team.colored())).collect(Collectors.joining("§7, "));
        String mark = messages.get(checklist.teamsDone() ? "setup-help-done" : "setup-help-missing");
        if (!checklist.teamRule().isChosen()) {
            line.add(ChatButton.text(messages.get("setup-help-teams", "mark", mark, "count", String.valueOf(checklist.teamCount()),
                "total", String.valueOf(checklist.teamRule().count()), "teams", names)));
            return line;
        }
        line.add(ChatButton.text(messages.get("setup-help-teams", "mark", mark, "count", String.valueOf(checklist.teamCount()),
            "total", String.valueOf(checklist.teamRule().count()), "teams", "")));
        for (int index = 0; index < draft.teams().size(); index++) {
            TeamColor team = draft.teams().get(index);
            String colored = Colors.translate(team.colored());
            line.add(ChatButton.run((index > 0 ? "§7, " : "") + colored, COMMAND + " time remover " + team.id(),
                messages.get("setup-help-team-remove-hover", "team", colored)));
        }
        if (!checklist.teamsDone()) {
            for (TeamColor team : TeamColor.values()) {
                if (!draft.teams().contains(team)) {
                    String colored = Colors.translate(team.colored());
                    line.add(ChatButton.run(messages.get("setup-help-team-add", "team", colored), COMMAND + " time adicionar " + team.id(),
                        messages.get("setup-help-team-add-hover", "team", colored)));
                }
            }
        }
        return line;
    }

    private List<ChatButton> item(Item item) {
        SetupStep step = item.step();
        String mark = messages.get(item.done() ? (item.count() == 0 ? "setup-help-optional" : "setup-help-done") : "setup-help-missing");
        String count = step.isList() ? messages.get("setup-help-count", "count", String.valueOf(item.count())) : "";
        List<ChatButton> line = new ArrayList<>();
        line.add(ChatButton.text(mark + messages.get("setup-help-item", "label", label(step, item.team()), "count", count)));
        String hint = messages.get("setup-help-hint-" + step.kind().name().toLowerCase(Locale.ROOT));
        if (step.isList()) {
            if (!item.full()) {
                line.add(ChatButton.run(messages.get("setup-help-add"), command("definir", step, item.team()), messages.get("setup-help-add-hover"), hint));
            }
        } else {
            line.add(ChatButton.run(messages.get("setup-help-define"), command("definir", step, item.team()), messages.get("setup-help-define-hover"), hint));
        }
        if (item.count() > 0) {
            line.add(ChatButton.run(messages.get("setup-help-remove"), command("remover", step, item.team()), messages.get("setup-help-remove-hover")));
        }
        return line;
    }

}
