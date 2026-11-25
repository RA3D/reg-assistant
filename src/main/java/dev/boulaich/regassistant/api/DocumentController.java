package dev.boulaich.regassistant.api;

import dev.boulaich.regassistant.dto.IngestionResult;
import dev.boulaich.regassistant.service.IngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final IngestionService ingestionService;

    public DocumentController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public IngestionResult upload(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName = StringUtils.getFilename(file.getOriginalFilename());
        if (file.isEmpty() || fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Please upload a non-empty PDF file");
        }
        return ingestionService.ingest(fileName, file.getBytes());
    }
}
