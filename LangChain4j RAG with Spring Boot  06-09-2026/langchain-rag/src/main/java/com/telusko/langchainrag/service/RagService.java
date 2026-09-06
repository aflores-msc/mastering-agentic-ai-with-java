package com.telusko.langchainrag.service;

import com.telusko.langchainrag.assistant.Assistant;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class RagService
{
    private final EmbeddingStoreIngestor ingestor;

    private final Assistant assistant;

    public RagService(EmbeddingStoreIngestor ingestor, Assistant assistant)
    {
        this.ingestor = ingestor;
        this.assistant = assistant;
    }

    @PostConstruct
    public void seedKnowledgeBase() throws Exception
    {
        var resolver =
                new PathMatchingResourcePatternResolver();
        Resource[] files =
                resolver.getResources("classpath:docs/*.txt");
        int count = 0;
        for(Resource file:files)
        {
            String text=new String(
                    file.getInputStream().readAllBytes(), StandardCharsets.UTF_8
            );
            // convert the text into langchian4j doc
            Document doc=Document.from(text);

            doc.metadata().put("source", file.getFilename());

            ingestor.ingest(doc);// doc --> split --> embed --> store
            count ++;
        }
        System.out.println(
                "[RAG] Ingested "
                        + count
                        + " document(s) into the vector store."
        );
    }

    public String ask(String question)
    {
        return assistant.answer(question);
    }

    // adding new knowledge at runtime --> text --> doc --> ingest
    public String ingestText(String text) {

        ingestor.ingest(
                Document.from(text)
        );
        return "Ingested "
                + text.length()
                + " characters into the vector store.";
    }

    public String ingestFile(String path)
    {
        String clean=path.trim();
        Document doc =FileSystemDocumentLoader.loadDocument(clean, new TextDocumentParser());
        ingestor.ingest(doc);
        return "Ingested file : "+ clean;
    }

}
