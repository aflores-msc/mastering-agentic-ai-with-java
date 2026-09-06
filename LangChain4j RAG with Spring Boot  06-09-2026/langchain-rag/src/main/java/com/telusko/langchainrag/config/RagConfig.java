package com.telusko.langchainrag.config;

import com.telusko.langchainrag.assistant.Assistant;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String modelName;

    // memory --> InMemoryEmbeddingStore
    //pgvector --> PostgreSQL + PGVector
    @Value("${rag.store}")
    private String storeType;

    @Value("${rag.pgvector.host}")
    private String pgHost;

    @Value("${rag.pgvector.port}")
    private int pgPort;

    @Value("${rag.pgvector.database}")
    private String pgDb;

    @Value("${rag.pgvector.user}")
    private String pgUser;

    @Value("${rag.pgvector.password}")
    private String pgPassword;

    @Value("${rag.pgvector.table}")
    private String pgTable;


    @Bean
    public ChatModel chatModel() {

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    // this is embedding model  --> converts our text into vectors
// this runs locaaly on jvm without api key and also 384 dimensional vector
    @Bean
    public EmbeddingModel embeddingModel() {

        return new BgeSmallEnV15QuantizedEmbeddingModel();
    }

    //Embedding store or vector db
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            EmbeddingModel embeddingModel) {

        if ("pgvector".equalsIgnoreCase(storeType)) {

            return PgVectorEmbeddingStore.builder()
                    .host(pgHost)
                    .port(pgPort)
                    .database(pgDb)
                    .user(pgUser)
                    .password(pgPassword)
                    .table(pgTable)
                    // BGE embedding model produces 384 dimensions.
                    .dimension(embeddingModel.dimension())
                    .build();
        }
        return new InMemoryEmbeddingStore<>();
    }


    //ingestion process --> doc -> chunks -> embeddings --> store embeddings

    @Bean
    public EmbeddingStoreIngestor ingestor(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(
                        DocumentSplitters.recursive(300, 30)
                )
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    // retrieval part of RAG  --> find most relevant chunks
    @Bean
    public ContentRetriever contentRetriever(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore)
            {
                return EmbeddingStoreContentRetriever.builder()
                        .embeddingStore(embeddingStore)
                        .embeddingModel(embeddingModel)
                        .maxResults(3)// most 3 relevant chunks
                        .minScore(0.5)// ignore results with weak similarity search
                        .build();
     }

     // RAG connection --> AI assistant
     @Bean
     public Assistant assistant(
             ChatModel chatModel,
             ContentRetriever contentRetriever)
     {
         return AiServices.builder(Assistant.class)
                 .chatModel(chatModel)
                 .contentRetriever(contentRetriever)
                 .chatMemory(
                         MessageWindowChatMemory.withMaxMessages(10)
                 )
                 .build();
     }


}
