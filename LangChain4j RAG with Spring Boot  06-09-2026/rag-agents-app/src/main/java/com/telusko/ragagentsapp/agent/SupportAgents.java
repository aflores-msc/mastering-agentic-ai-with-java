package com.telusko.ragagentsapp.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public final class SupportAgents
{
    private SupportAgents()
    {
    }
    public interface PolicyExpert {
        @UserMessage("""
                Answer the customer's question using ONLY the retrieved company policy.
                If the policy does not cover it, say so. Question: {{request}}
                """)
        @Agent("Answers questions about refund, return, shipping, and warranty policy from the company documents")
        String answer(@V("request") String request);
    }

    public interface OrderAgent {
        @UserMessage("""
                Use your tools to look up the order details needed for this request, and report the
                concrete facts you found (status, days since delivery, whether it is defective).
                Request: {{request}}
                """)
        @Agent("Looks up an order's status, delivery date, and defect flag using tools")
        String handle(@V("request") String request);
    }
}
