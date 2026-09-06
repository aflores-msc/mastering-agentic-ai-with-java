package com.telusko.officeagentictools.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public final class AssistantAgents
{
    private AssistantAgents()
    {
    }

    public interface OrdersAgent
    {

        // Prompt given to this agent whenever it is called.
        @UserMessage("""
                You handle order lookups. Use your tools to answer this request, and reply with
                the concrete numbers you found. Request: {{request}}
                """)

        // The Supervisor reads THIS description while planning.
        @Agent("Looks up the customer's orders and their totals")

        // request -> user's original sentence
        String handle(@V("request") String request);
    }

        public interface FinanceAgent
        {
            @UserMessage("""
                You do money maths (tax, tips, totals). Use your tools to compute exact amounts.
                Request: {{request}}
                """)
            @Agent("Does money calculations such as adding tax or a tip to an amount")
            String calculate(@V("request") String request);
        }

        public interface ReminderAgent {
            @UserMessage("""
                You set reminders. Use your tools to create the reminder and confirm it.
                Request: {{request}}
                """)
            @Agent("Sets reminders for tasks on a given day")
            String remind(@V("request") String request);
        }
    }



