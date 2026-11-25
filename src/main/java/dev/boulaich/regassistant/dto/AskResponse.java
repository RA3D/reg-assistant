package dev.boulaich.regassistant.dto;

import java.util.List;

public record AskResponse(String answer, List<Source> sources) {

    public record Source(String file, int page, double score) {
    }
}
