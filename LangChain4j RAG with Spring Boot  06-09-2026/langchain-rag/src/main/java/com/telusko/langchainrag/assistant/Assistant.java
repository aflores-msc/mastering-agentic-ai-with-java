package com.telusko.langchainrag.assistant;

import dev.langchain4j.service.SystemMessage;

public interface Assistant {

    @SystemMessage("""
            You are a helpful assistant for the Telusko AI course.
            Answer using ONLY the provided context. If the answer is not in the context,
            say you do not know. Keep answers short and clear.
            """)
    String answer (String question);
}
