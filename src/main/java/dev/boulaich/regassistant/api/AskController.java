package dev.boulaich.regassistant.api;

import dev.boulaich.regassistant.dto.AskRequest;
import dev.boulaich.regassistant.dto.AskResponse;
import dev.boulaich.regassistant.service.AskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ask")
public class AskController {

    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    @PostMapping
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return askService.ask(request.question());
    }
}
