package com.telusko.travelagenticsystem.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public final class TravelAgents
{
    private TravelAgents()
    {
    }
    public interface DestinationExpert
    {
        @UserMessage("""
                For this trip, suggest the best things to see and do.
                Give a short bulleted list (max 5). Trip: {{request}}
                """)
        @Agent("Suggests attractions and activities for a destination")
        String suggest(@V("request") String request);
    }


    public interface WeatherAdvisor
    {
        @UserMessage("""
                Describe the typical weather to expect for this trip in two short sentences.
                Trip: {{request}}
                """)
        @Agent("Describes the expected weather for the destination and time of year")
        String weather(@V("request") String request);
    }


    public interface PackingAssistant
    {
        @UserMessage("""
                Suggest a short packing list (max 8 items) for this trip. Keep the weather in mind.
                Trip: {{request}}
                """)
        @Agent("Creates a packing list for the trip")
        String pack(@V("request") String request);
    }


    public interface BudgetPlanner
    {
        @UserMessage("""
                Give a rough budget estimate and 3 money-saving tips for this trip. Keep it short.
                Trip: {{request}}
                """)
        @Agent("Estimates the budget and gives money-saving tips")
        String budget(@V("request") String request);
    }

}
