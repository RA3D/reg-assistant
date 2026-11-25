package dev.boulaich.regassistant.service;

import dev.boulaich.regassistant.dto.IngestionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Reads a PDF page by page, splits pages into chunks and stores them in PGVector.
 * The reader puts "file_name" and "page_number" in the metadata of each page,
 * and the splitter copies that metadata to every chunk, which is what lets us cite pages later.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public IngestionResult ingest(String fileName, byte[] content) {
        // ByteArrayResource has no file name by default, and the reader uses it for the "file_name" metadata
        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        List<Document> pages = new PagePdfDocumentReader(resource).get();
        if (pages.isEmpty()) {
            throw new IllegalArgumentException("No text found in " + fileName + " (scanned PDF?)");
        }

        List<Document> chunks = splitter.apply(pages);
        vectorStore.add(chunks);

        log.info("Ingested {}: {} pages, {} chunks", fileName, pages.size(), chunks.size());
        return new IngestionResult(fileName, pages.size(), chunks.size());
    }
}
