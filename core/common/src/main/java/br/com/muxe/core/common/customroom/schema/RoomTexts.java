package br.com.muxe.core.common.customroom.schema;

import java.util.HashMap;
import java.util.Map;

public record RoomTexts(Map<String, String> texts) {

    public static final Map<String, String> DEFAULTS = Map.ofEntries(
        Map.entry("value", "&7Valor: &a{value}"),
        Map.entry("value-plain", "&7Valor: {value}"),
        Map.entry("left-increase", "&eBotão esquerdo: &fAumentar"),
        Map.entry("right-decrease", "&eBotão direito: &fDiminuir"),
        Map.entry("click-change", "&eClique para alterar."),
        Map.entry("click-enable", "&eClique para ativar."),
        Map.entry("click-disable", "&eClique para desativar."),
        Map.entry("click-switch", "&eClique para trocar."),
        Map.entry("enabled", "&aAtivado"),
        Map.entry("disabled", "&cDesativado"),
        Map.entry("choice-selected", "&a- {label}"),
        Map.entry("choice-other", "&7- {label}"),
        Map.entry("time", "&7Tempo: &f{time}"),
        Map.entry("status", "&7Status: {status}"),
        Map.entry("timer-left", "&eBotão esquerdo: &f+30s"),
        Map.entry("timer-right", "&eBotão direito: &f-30s"),
        Map.entry("timer-shift", "&eShift: &f±1min"),
        Map.entry("timer-drop", "&eQ: &fAtivar/desativar"),
        Map.entry("team-name", "Time #{index}"),
        Map.entry("team-original", "&7Nome original: &f{name}"),
        Map.entry("team-toggle", "&eBotão esquerdo: &fAtivar/desativar"),
        Map.entry("team-rename", "&eBotão direito: &fRenomear"),
        Map.entry("team-restore", "&eQ: &fRestaurar nome"),
        Map.entry("price", "&7Preço: {price}"),
        Map.entry("free", "&aGrátis"),
        Map.entry("limit", "&7Limite: &f{limit}"),
        Map.entry("limit-none", "Nenhum"),
        Map.entry("limit-team", "{amount} por time"),
        Map.entry("limit-player", "{amount} por jogador"),
        Map.entry("limit-inventory", "{amount} no inventário"),
        Map.entry("changed", "*"),
        Map.entry("status-name", "&aStatus"),
        Map.entry("price-name", "&aPreço"),
        Map.entry("limit-name", "&aLimite"),
        Map.entry("change-value", "&eBotão esquerdo: &fAlterar valor"),
        Map.entry("change-currency", "&eBotão direito: &fAlterar moeda"),
        Map.entry("change-amount", "&eBotão esquerdo: &fAlterar quantidade"),
        Map.entry("change-type", "&eBotão direito: &fAlterar tipo"),
        Map.entry("level-name", "&aNível {level}"),
        Map.entry("level-line", "&7Nível {level}: {price}"),
        Map.entry("level-label", "&7{label}"),
        Map.entry("free-on-name", "&aTudo grátis"),
        Map.entry("free-off-name", "&cTudo grátis"),
        Map.entry("free-on-click", "&eClique para voltar ao preço padrão."),
        Map.entry("free-off-click", "&eClique para deixar tudo grátis."),
        Map.entry("back-name", "&aVoltar"),
        Map.entry("back-lore", "&7Para {title}"),
        Map.entry("page-name", "&aPágina {page}"),
        Map.entry("anvil-result", "&a{label}: &f{value}"),
        Map.entry("anvil-result-plain", "&f{value}"),
        Map.entry("editing-title", "Editando {name}..."),
        Map.entry("blank", " ")
    );

    public RoomTexts {
        Map<String, String> merged = new HashMap<>(DEFAULTS);
        merged.putAll(texts);
        texts = Map.copyOf(merged);
    }

    public static RoomTexts defaults() {
        return new RoomTexts(Map.of());
    }

    public String get(String key, String... placeholders) {
        String text = texts.getOrDefault(key, key);
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            text = text.replace("{" + placeholders[index] + "}", placeholders[index + 1]);
        }
        return text;
    }

    public static String roman(int number) {
        String[] numerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return number >= 1 && number <= numerals.length ? numerals[number - 1] : String.valueOf(number);
    }
}
