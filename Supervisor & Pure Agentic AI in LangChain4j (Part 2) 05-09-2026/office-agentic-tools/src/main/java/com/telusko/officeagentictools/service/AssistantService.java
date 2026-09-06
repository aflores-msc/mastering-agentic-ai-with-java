package com.telusko.officeagentictools.service;

import com.telusko.officeagentictools.agents.AssistantAgents.*;
import com.telusko.officeagentictools.tools.OfficeTools;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class AssistantService
{
    private ChatModel chatModel;

    private final OfficeTools tools = new OfficeTools();
    public AssistantService(ChatModel chatModel)
    {
        this.chatModel = chatModel;
    }
    public String assist(String request)
    {
        OrdersAgent orders = AgenticServices.agentBuilder(OrdersAgent.class)
                .chatModel(chatModel)
                // Give this agent access to OfficeTools
                .tools(tools)
                // Shared key used inside the Supervisor context
                .outputKey("orders")
                .build();
        FinanceAgent finance = AgenticServices.agentBuilder(FinanceAgent.class)
                .chatModel(chatModel)
                .tools(tools)
                .outputKey("finance")
                .build();

        ReminderAgent reminder = AgenticServices.agentBuilder(ReminderAgent.class)
                .chatModel(chatModel)
                .tools(tools)
                .outputKey("reminder")
                .build();

        //create supervisor
        SupervisorAgent assistant = AgenticServices.supervisorBuilder()
                .chatModel(chatModel)
                .subAgents(orders, finance, reminder)
                .supervisorContext(
                        """
                        You are an office assistant. Break the user's request into steps and use the
                        specialist agents to carry them out. If one step needs a number from an earlier
                        step (for example an order total before adding a tip), do the lookup first and
                        pass the number along. Finish with one clear summary for the user.
                        """
                )
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .maxAgentsInvocations(8)
                .build();

    return assistant.invoke(request);
    }
}
