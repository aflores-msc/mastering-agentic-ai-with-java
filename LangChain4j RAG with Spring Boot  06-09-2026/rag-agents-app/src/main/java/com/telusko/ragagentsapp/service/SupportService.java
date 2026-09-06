package com.telusko.ragagentsapp.service;

import com.telusko.ragagentsapp.agent.SupportAgents.*;
import com.telusko.ragagentsapp.tools.OrderTools;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class SupportService
{
    private final ChatModel chatModel;
    private final ContentRetriever policyRetriever;
    private final EmbeddingStoreIngestor ingestor;

    private SupervisorAgent supportDesk;

    public SupportService(ChatModel chatModel, ContentRetriever policyRetriever,
                          EmbeddingStoreIngestor ingestor) {
        this.chatModel = chatModel;
        this.policyRetriever = policyRetriever;
        this.ingestor = ingestor;
    }


    @PostConstruct
    public void setup()throws Exception
    {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] files = resolver.getResources("classpath:policies/*.txt");
        for (Resource file : files) {
            String text = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Document doc = Document.from(text);
            doc.metadata().put("source", file.getFilename());
            ingestor.ingest(doc);
        }
        System.out.println("[SUPPORT] Loaded " + files.length + " policy document(s) into PGVector.");

        PolicyExpert policyExpert = AgenticServices.agentBuilder(PolicyExpert.class)
                .chatModel(chatModel)
                .contentRetriever(policyRetriever)      // <-- RAG lives here
                .outputKey("policy")
                .build();

        OrderAgent orderAgent = AgenticServices.agentBuilder(OrderAgent.class)
                .chatModel(chatModel)
                .tools(new OrderTools())                // <-- tools live here
                .outputKey("order")
                .build();
        this.supportDesk=AgenticServices.supervisorBuilder()
                .chatModel(chatModel)
                .subAgents(policyExpert, orderAgent)
                .supervisorContext(
                        """
                                     You are a customer support desk. Use the policy expert for questions about refund,
                                     return, shipping, or warranty policy, and the order agent to look up an order's
                                     status, delivery date, or defect. For a refund or return request, FIRST get the
                                     order facts, THEN check them against the policy, and finish with one clear decision
                                     (approve, decline, or what the customer should do next), with the reason.
                                     """
                )
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .maxAgentsInvocations(8)
                .build();
    }

    public String handle(String request)
    {
        return supportDesk.invoke(request);
    }

}
