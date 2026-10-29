package br.com.muxe.core.common.shop.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record Promotion(String productId, List<String> lines) {

    public Promotion {
        Objects.requireNonNull(productId, "productId");
        if (productId.isBlank()) {
            throw new IllegalArgumentException("A promotion needs a product");
        }
        lines = List.copyOf(lines);
    }

    public Promotion featuring(String product) {
        return new Promotion(product, lines);
    }

    public Promotion adding(String line) {
        List<String> changed = new ArrayList<>(lines);
        changed.add(line);
        return new Promotion(productId, changed);
    }

    public Promotion replacing(int number, String line) {
        List<String> changed = new ArrayList<>(lines);
        changed.set(index(number), line);
        return new Promotion(productId, changed);
    }

    public Promotion without(int number) {
        List<String> changed = new ArrayList<>(lines);
        changed.remove(index(number));
        return new Promotion(productId, changed);
    }

    public boolean hasLine(int number) {
        return number >= 1 && number <= lines.size();
    }

    private int index(int number) {
        if (!hasLine(number)) {
            throw new IllegalArgumentException("Line " + number + " does not exist, the promotion has " + lines.size());
        }
        return number - 1;
    }
}
