package dev.boulaich.regassistant.service;

import dev.boulaich.regassistant.dto.AskResponse;
import dev.boulaich.regassistant.dto.AskResponse.Source;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AskService {

    static final String NO_ANSWER = "I don't know based on the provided documents.";

    private static final String SYSTEM_PROMPT = """
            You answer questions about banking regulation (EBA guidelines, EU regulations).

            Rules:
            - Use ONLY the context given in the user message. Never use outside knowledge, even if you know the answer.
            - Each context block starts with a tag like [file.pdf, p.12].
            - End every factual sentence with the tag(s) of the block(s) it comes from, exactly as written.
            - If the context does not contain the answer, reply exactly: "%s"
            - Be concise. Mention article or paragraph numbers when the context gives them.
            """.formatted(NO_ANSWER);

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;

    public AskService(ChatModel chatModel,
                      VectorStore vectorStore,
                      @Value("${reg-assistant.retrieval.top-k:5}") int topK,
                      @Value("${reg-assistant.retrieval.similarity-threshold:0.4}") double similarityThreshold) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public AskResponse ask(String question) {
        List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build());

        // Nothing relevant retrieved: no need to call the model at all
        if (docs.isEmpty()) {
            return new AskResponse(NO_ANSWER, List.of());
        }

        String userMessage = "Context:\n\n" + buildContext(docs) + "\n\nQuestion: " + question;

        // Plain messages on purpose: PDF text can contain { } which would break prompt templating
        Prompt prompt = new Prompt(List.of(new SystemMessage(SYSTEM_PROMPT), new UserMessage(userMessage)));
        String answer = chatModel.call(prompt).getResult().getOutput().getText();

        return new AskResponse(answer, toSources(docs));
    }

    static String buildContext(List<Document> docs) {
        return docs.stream()
                .map(doc -> "[" + fileOf(doc) + ", p." + pageOf(doc) + "]\n" + doc.getText().strip())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /** One source per (file, page). Results come sorted by score, so the first one we see is the best. */
    static List<Source> toSources(List<Document> docs) {
        Map<String, Source> sources = new LinkedHashMap<>();
        for (Document doc : docs) {
            double score = doc.getScore() == null ? 0.0 : Math.round(doc.getScore() * 1000) / 1000.0;
            Source source = new Source(fileOf(doc), pageOf(doc), score);
            sources.putIfAbsent(source.file() + "#" + source.page(), source);
        }
        return List.copyOf(sources.values());
    }

    // "file_name" and "page_number" are the metadata keys written by PagePdfDocumentReader
    private static String fileOf(Document doc) {
        Object file = doc.getMetadata().get("file_name");
        return file == null ? "unknown" : file.toString();
    }

    private static int pageOf(Document doc) {
        Object page = doc.getMetadata().get("page_number");
        if (page instanceof Number n) {
            return n.intValue();
        }
        return page == null ? 0 : Integer.parseInt(page.toString());
    }
}
