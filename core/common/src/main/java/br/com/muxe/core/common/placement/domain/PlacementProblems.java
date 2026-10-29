package br.com.muxe.core.common.placement.domain;

import br.com.muxe.core.common.result.Problem;

public final class PlacementProblems {

    public static final String NOT_FOUND = "placement.not_found";

    private PlacementProblems() {
    }

    public static Problem notFound(PlacementKind kind, String group, String id) {
        return Problem.of(NOT_FOUND, kind + " '" + id + "' does not exist in group '" + group + "'");
    }
}
