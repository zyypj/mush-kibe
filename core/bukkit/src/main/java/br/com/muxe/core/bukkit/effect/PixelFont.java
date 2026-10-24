package br.com.muxe.core.bukkit.effect;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class PixelFont {

    public static final int WIDTH = 5;
    public static final int HEIGHT = 7;

    private static final PixelFont STANDARD = new PixelFont(standardGlyphs());

    private final Map<Character, boolean[][]> glyphs;

    private PixelFont(Map<Character, String[]> rows) {
        this.glyphs = new HashMap<>();
        rows.forEach((character, lines) -> glyphs.put(character, parse(character, lines)));
    }

    public static PixelFont standard() {
        return STANDARD;
    }

    public boolean supports(char character) {
        return glyphs.containsKey(Character.toUpperCase(character));
    }

    public Optional<boolean[][]> glyph(char character) {
        boolean[][] glyph = glyphs.get(Character.toUpperCase(character));
        if (glyph == null) {
            return Optional.empty();
        }
        boolean[][] copy = new boolean[HEIGHT][];
        for (int row = 0; row < HEIGHT; row++) {
            copy[row] = glyph[row].clone();
        }
        return Optional.of(copy);
    }

    public boolean supportsAll(String text) {
        return text.toUpperCase(Locale.ROOT).chars().allMatch(character -> supports((char) character));
    }

    private static boolean[][] parse(char character, String[] lines) {
        if (lines.length != HEIGHT) {
            throw new IllegalStateException("Glyph '" + character + "' must have " + HEIGHT + " rows");
        }
        boolean[][] pixels = new boolean[HEIGHT][WIDTH];
        for (int row = 0; row < HEIGHT; row++) {
            if (lines[row].length() != WIDTH) {
                throw new IllegalStateException("Glyph '" + character + "' row " + row + " must have " + WIDTH + " columns");
            }
            for (int column = 0; column < WIDTH; column++) {
                pixels[row][column] = lines[row].charAt(column) == '#';
            }
        }
        return pixels;
    }

    private static Map<Character, String[]> standardGlyphs() {
        Map<Character, String[]> glyphs = new HashMap<>();
        glyphs.put(' ', new String[] {".....", ".....", ".....", ".....", ".....", ".....", "....."});
        glyphs.put('0', new String[] {".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."});
        glyphs.put('1', new String[] {"..#..", ".##..", "#.#..", "..#..", "..#..", "..#..", "#####"});
        glyphs.put('2', new String[] {".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"});
        glyphs.put('3', new String[] {".###.", "#...#", "....#", "..##.", "....#", "#...#", ".###."});
        glyphs.put('4', new String[] {"...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."});
        glyphs.put('5', new String[] {"#####", "#....", "####.", "....#", "....#", "#...#", ".###."});
        glyphs.put('6', new String[] {".###.", "#....", "#....", "####.", "#...#", "#...#", ".###."});
        glyphs.put('7', new String[] {"#####", "....#", "...#.", "..#..", "..#..", "..#..", "..#.."});
        glyphs.put('8', new String[] {".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."});
        glyphs.put('9', new String[] {".###.", "#...#", "#...#", ".####", "....#", "....#", ".###."});
        glyphs.put('A', new String[] {".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"});
        glyphs.put('B', new String[] {"####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."});
        glyphs.put('C', new String[] {".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."});
        glyphs.put('D', new String[] {"####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."});
        glyphs.put('E', new String[] {"#####", "#....", "#....", "####.", "#....", "#....", "#####"});
        glyphs.put('F', new String[] {"#####", "#....", "#....", "####.", "#....", "#....", "#...."});
        glyphs.put('G', new String[] {".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####"});
        glyphs.put('H', new String[] {"#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"});
        glyphs.put('I', new String[] {".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###."});
        glyphs.put('J', new String[] {"..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##.."});
        glyphs.put('K', new String[] {"#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"});
        glyphs.put('L', new String[] {"#....", "#....", "#....", "#....", "#....", "#....", "#####"});
        glyphs.put('M', new String[] {"#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"});
        glyphs.put('N', new String[] {"#...#", "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#"});
        glyphs.put('O', new String[] {".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."});
        glyphs.put('P', new String[] {"####.", "#...#", "#...#", "####.", "#....", "#....", "#...."});
        glyphs.put('Q', new String[] {".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#"});
        glyphs.put('R', new String[] {"####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"});
        glyphs.put('S', new String[] {".####", "#....", "#....", ".###.", "....#", "....#", "####."});
        glyphs.put('T', new String[] {"#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."});
        glyphs.put('U', new String[] {"#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."});
        glyphs.put('V', new String[] {"#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#.."});
        glyphs.put('W', new String[] {"#...#", "#...#", "#...#", "#.#.#", "#.#.#", "#.#.#", ".#.#."});
        glyphs.put('X', new String[] {"#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#"});
        glyphs.put('Y', new String[] {"#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."});
        glyphs.put('Z', new String[] {"#####", "....#", "...#.", "..#..", ".#...", "#....", "#####"});
        return glyphs;
    }
}
