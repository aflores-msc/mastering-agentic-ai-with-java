package com.telusko.ragagentsapp.config;

import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig
{
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String modelName;

    @Value("${pgvector.host}")
    private String pgHost;

    @Value("${pgvector.port}")
    private int pgPort;

    @Value("${pgvector.database}")
    private String pgDb;

    @Value("${pgvector.user}")
    private String pgUser;

    @Value("${pgvector.password}")
    private String pgPassword;

    @Value("${pgvector.table}")
    private String pgTable;

    @Bean
    public ChatModel chatModel() {

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return new BgeSmallEnV15QuantizedEmbeddingModel();
    }

    // PG Vector config
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) {
        return PgVectorEmbeddingStore.builder()
                .host(pgHost)
                .port(pgPort)
                .database(pgDb)
                .user(pgUser)
                .password(pgPassword)
                .table(pgTable)
                .dimension(embeddingModel.dimension())
                .build();
    }
// load our doc into PGVector store  --> split --> embed --> store
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

    // retrieval part of RAG  --> find most relevant chunks policy relevant policies
    @Bean
    public ContentRetriever policyContentRetriever(
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
}
