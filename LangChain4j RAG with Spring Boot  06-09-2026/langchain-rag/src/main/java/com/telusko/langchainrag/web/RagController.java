package com.telusko.langchainrag.web;

import com.telusko.langchainrag.service.RagService;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api-rag")
@RestController
public class RagController
{
    private RagService service;

    public RagController(RagService service)
    {
        this.service = service;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question)
    {
        return service.ask(question);
    }

    @PostMapping("/ingest")
    public String ingest(@RequestBody String text)
    {
        return service.ingestText(text);
    }

    @GetMapping("/ingest-file")
    public String ingestFile(@RequestParam String path)
    {
        return service.ingestFile(path);
    }
}
