package br.com.muxe.core.bukkit.party;

import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.party.domain.PartyNotice;
import br.com.muxe.core.common.party.domain.PartySnapshot;
import java.util.List;
import java.util.stream.Collectors;

public final class PartyTexts {

    private final MessageCatalog messages;
    private final String chatFormat;

    public PartyTexts(MessageCatalog messages, String chatFormat) {
        this.messages = messages;
        this.chatFormat = chatFormat;
    }

    public MessageCatalog messages() {
        return messages;
    }

    public List<String> lines(PartyNotice notice) {
        return lines(Language.DEFAULT, notice);
    }

    public List<String> lines(Language language, PartyNotice notice) {
        if (notice.isRaw()) {
            return List.of(notice.raw());
        }
        List<String> raw = notice.placeholders();
        String[] values = new String[raw.size()];
        for (int index = 0; index < raw.size(); index++) {
            values[index] = index % 2 == 0 ? raw.get(index) : Colors.translate(raw.get(index));
        }
        return messages.lines(language, notice.key(), (Object[]) values);
    }

    public String members(PartySnapshot party) {
        return members(Language.DEFAULT, party);
    }

    public String members(Language language, PartySnapshot party) {
        String separator = messages.get(language, "party-members-separator");
        String names = party.members().stream()
            .map(member -> messages.get(language, member.online() ? "party-members-online" : "party-members-offline", "name", member.name()))
            .collect(Collectors.joining(separator));
        return messages.get(language, "party-members", "count", String.valueOf(party.members().size()), "members", names);
    }

    public String chat(RankDisplay display, String player, String message) {
        return messages.get("party-chat", "chat", String.format(display.chatFormat(chatFormat), player, message));
    }
}
