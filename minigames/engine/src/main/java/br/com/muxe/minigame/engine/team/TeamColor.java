package br.com.muxe.minigame.engine.team;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Text;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum TeamColor {
    AZUL("azul", "Azul", "Blue", '9', 11, 0x3C44AA),
    AMARELO("amarelo", "Amarelo", "Yellow", 'e', 4, 0xFED83D),
    CIANO("ciano", "Ciano", "Aqua", 'b', 9, 0x169C9C),
    CINZA("cinza", "Cinza", "Gray", '8', 7, 0x474F52),
    VERMELHO("vermelho", "Vermelho", "Red", 'c', 14, 0xB02E26),
    ROSA("rosa", "Rosa", "Pink", 'd', 6, 0xF38BAA),
    VERDE("verde", "Verde", "Green", 'a', 5, 0x80C71F),
    BRANCO("branco", "Branco", "White", 'f', 0, 0xF9FFFE),
    LARANJA("laranja", "Laranja", "Orange", '6', 1, 0xF9801D);

    private final String id;
    private final String displayName;
    private final String englishName;
    private final char colorCode;
    private final int woolData;
    private final int rgb;

    TeamColor(String id, String displayName, String englishName, char colorCode, int woolData, int rgb) {
        this.id = id;
        this.displayName = displayName;
        this.englishName = englishName;
        this.colorCode = colorCode;
        this.woolData = woolData;
        this.rgb = rgb;
    }

    public static Optional<TeamColor> find(String id) {
        String wanted = id.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(color -> color.id.equals(wanted)).findFirst();
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String displayName(Language language) {
        return language == Language.ENGLISH ? englishName : displayName;
    }

    public Text label() {
        return Text.of(displayName, englishName);
    }

    public String color() {
        return "&" + colorCode;
    }

    public String colored() {
        return color() + displayName;
    }

    public char letter() {
        return displayName.charAt(0);
    }

    public String boldLetter() {
        return "&l" + letter() + color();
    }

    public int woolData() {
        return woolData;
    }

    public int rgb() {
        return rgb;
    }
}
