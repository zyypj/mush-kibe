package br.com.muxe.core.bukkit.display;

import java.util.ArrayList;
import java.util.List;

public final class LabelStack {

    private LabelStack() {
    }

    public static List<DisplayLabel> downward(List<String> lines, double top, double spacing) {
        requirePositive(spacing);
        List<DisplayLabel> labels = new ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            labels.add(new DisplayLabel(lines.get(index), top - index * spacing));
        }
        return labels;
    }

    public static List<DisplayLabel> upward(List<String> lines, double bottom, List<Double> gaps, double spacing) {
        requirePositive(spacing);
        double[] heights = new double[lines.size()];
        double height = bottom;
        for (int fromBottom = 0; fromBottom < lines.size(); fromBottom++) {
            if (fromBottom > 0) {
                height += fromBottom - 1 < gaps.size() ? gaps.get(fromBottom - 1) : spacing;
            }
            heights[lines.size() - 1 - fromBottom] = height;
        }
        List<DisplayLabel> labels = new ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            labels.add(new DisplayLabel(lines.get(index), heights[index]));
        }
        return labels;
    }

    private static void requirePositive(double spacing) {
        if (spacing <= 0) {
            throw new IllegalArgumentException("Line spacing must be positive, got " + spacing);
        }
    }
}
