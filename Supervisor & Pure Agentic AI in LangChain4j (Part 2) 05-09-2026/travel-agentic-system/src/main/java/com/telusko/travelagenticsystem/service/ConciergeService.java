package com.telusko.travelagenticsystem.service;

import com.telusko.travelagenticsystem.agents.TravelAgents.*;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.HtmlReportGenerator;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class ConciergeService
{
    private final ChatModel chatModel;

    public ConciergeService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    private DestinationExpert destination() {
        return AgenticServices.agentBuilder(DestinationExpert.class)
                .chatModel(chatModel)
                .outputKey("activities")
                .build();
    }

        private WeatherAdvisor weather() {
            return AgenticServices.agentBuilder(WeatherAdvisor.class)
                    .chatModel(chatModel)
                    .outputKey("weather")
                    .build();
        }
    private PackingAssistant packing() {
        return AgenticServices.agentBuilder(PackingAssistant.class)
                .chatModel(chatModel)
                .outputKey("packing")
                .build();
    }

    private BudgetPlanner budget() {
        return AgenticServices.agentBuilder(BudgetPlanner.class)
                .chatModel(chatModel)
                .outputKey("budget")
                .build();
    }
//Pure Agentic approach

    public String concierge(String request, String strategyName)
    {
        // Convert the String we got from API into required type of SupervisorResponseStrategy
        SupervisorResponseStrategy strategy = parseStrategy(strategyName);

        //Create Supervisor agent
        SupervisorAgent concierge = AgenticServices
                .supervisorBuilder()
                .chatModel(chatModel)
                .subAgents(
                        destination(),
                        weather(),
                        packing(),
                        budget())
                .supervisorContext(
                        """
                                You are a travel concierge. Call only the specialist agents the user's
                                request actually needs. Then give one clear, friendly answer.
                                """
                )
                .contextGenerationStrategy(SupervisorContextStrategy.SUMMARIZATION)
                .responseStrategy(strategy)
                //safety limit --> max no of agents invocation
                .maxAgentsInvocations(6)
                .build();

        // start Supervisor with users request
        return concierge.invoke(request);
    }

    private SupervisorResponseStrategy parseStrategy(String name)
    {
        if (name == null || name.isBlank()) {
            return SupervisorResponseStrategy.SUMMARY;
        }
        try {

            /*
             * Convert the supplied String to uppercase
             * and find the corresponding enum value.
             */
            return SupervisorResponseStrategy.valueOf(name.trim().toUpperCase());

        } catch (IllegalArgumentException e) {

            /*
             * If somebody gives an invalid strategy,
             * safely fall back to SUMMARY.
             */
            return SupervisorResponseStrategy.SUMMARY;
        }
    }

    // Deterministic workflow
    //we will call the agents by our self
    public String deterministic(String request) {

        /*
         * We explicitly call the Destination agent.
         */
        String activities = destination().suggest(request);

        /*
         * We explicitly call the Weather agent.
         */
        String weather = weather().weather(request);

        /*
         * We explicitly call the Packing agent.
         */
        String packing = packing().pack(request);

        /*
         * We explicitly call the Budget agent.
         */
        String budget = budget().budget(request);

        /*
         * Now we manually combine all four responses.
         */
        return "ACTIVITIES:\n" + activities
                + "\n\nWEATHER:\n" + weather
                + "\n\nPACKING:\n" + packing
                + "\n\nBUDGET:\n" + budget;
    }

    public String withReport(String request)
    {
        AgentMonitor monitor = new AgentMonitor();

        SupervisorAgent concierge = AgenticServices.supervisorBuilder()
                .chatModel(chatModel)
                .subAgents(destination(), weather(), packing(), budget())
                .supervisorContext("You are a travel concierge. Call only the agents the request needs.")
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .maxAgentsInvocations(6)
                .listener(monitor)
                .build();

        String answer = concierge.invoke(request);

        String reportPath;

        try {
            Path path = Path.of("agent-report.html");
            HtmlReportGenerator.generateReport(monitor, path);
            reportPath = path.toAbsolutePath().toString();
        } catch (Exception e) {
            reportPath = "(report could not be generated: " + e.getMessage() + ")";
        }

        return "ANSWER:\n" + answer + "\n\nHTML REPORT: " + reportPath;

        }


    }



