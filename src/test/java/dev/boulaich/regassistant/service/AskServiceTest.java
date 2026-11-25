package dev.boulaich.regassistant.service;

import dev.boulaich.regassistant.dto.AskResponse.Source;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AskServiceTest {

    @Test
    void buildContext_tagsEachChunkWithFileAndPage() {
        List<Document> docs = List.of(
                chunk("  Institutions should maintain a register of outsourcing arrangements.  ", "EBA-GL-2019-02.pdf", 14, 0.82),
                chunk("Major ICT-related incidents shall be reported.", "DORA.pdf", 37, 0.75));

        String context = AskService.buildContext(docs);

        assertThat(context).isEqualTo("""
                [EBA-GL-2019-02.pdf, p.14]
                Institutions should maintain a register of outsourcing arrangements.

                ---

                [DORA.pdf, p.37]
                Major ICT-related incidents shall be reported.""");
    }

    @Test
    void buildContext_handlesMissingMetadata() {
        Document doc = Document.builder().text("Some text").build();

        assertThat(AskService.buildContext(List.of(doc))).isEqualTo("[unknown, p.0]\nSome text");
    }

    @Test
    void toSources_keepsBestScorePerPage() {
        List<Document> docs = List.of(
                chunk("a", "DORA.pdf", 37, 0.8123),
                chunk("b", "DORA.pdf", 37, 0.70),
                chunk("c", "DORA.pdf", 38, 0.65));

        assertThat(AskService.toSources(docs)).containsExactly(
                new Source("DORA.pdf", 37, 0.812),
                new Source("DORA.pdf", 38, 0.65));
    }

    private static Document chunk(String text, String file, int page, double score) {
        return Document.builder()
                .text(text)
                .metadata(Map.of("file_name", file, "page_number", page))
                .score(score)
                .build();
    }
}
