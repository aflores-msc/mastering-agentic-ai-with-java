package com.telusko.agenticaisystem.web;

import com.telusko.agenticaisystem.agents.Review;
import com.telusko.agenticaisystem.service.AgenticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/agentic/api")
@RestController
public class AgenticController
{
    @Autowired
    private AgenticService service;

    @GetMapping("/basic")
    public String basicAgentApi(@RequestParam String topic)
    {
        return service.basicAgent(topic);
    }

    @GetMapping("/sequential")
    public String seqAgentApi(@RequestParam String topic,
                              @RequestParam(defaultValue = " a general audience") String audience)
    {
        return service.sequential(topic, audience);
    }

    @GetMapping("/loop")
    public String loopAgentApi(@RequestParam String story)
    {
        return service.loop(story);
    }

    @GetMapping("/parallel")
    public Review parallelAgentApi(@RequestParam String text)
    {
        return service.parallelAgents(text);
    }

    @GetMapping("/mapper")
    public Object parallelAgentApi(@RequestParam List<String> topics)
    {
        return service.mapper(topics);
    }

    @GetMapping("/conditional")
    public String conditionalAgentApi(@RequestParam String message)
    {
        String res=service.condionalAgents(message);
        System.out.println(res);
        return res;
    }
    @GetMapping("/optional-async")
    public Object optionalAgentApi(@RequestParam String topic)
    {
        return service.optionalAsync(topic);

    }


    @GetMapping("/streaming")
    public String streaming(
            @RequestParam String topic) {

        // Call the streaming service method.
        return service.streaming(topic);
    }

    @GetMapping("/error")
    public String error() {

        // Start the error-recovery workflow.
        return service.errorRecovery();
    }

    @GetMapping("/observability")
    public String observability(
            @RequestParam String topic) {

        return service.observability(topic);
    }
    @GetMapping("/human")
    public String human(

            @RequestParam String request,

            // If no decision is supplied,
            // use "Approve".
            @RequestParam(
                    defaultValue = "Approve"
            )
            String decision) {

        return service.human(
                request,
                decision
        );
    }


    @GetMapping("/nonai")
    public String nonAi(
            @RequestParam String topic) {

        // Pass the topic into the mixed AI + Java workflow.
        return service.nonAi(topic);
    }

    @GetMapping("/declarative")
    public String declarative(
            @RequestParam String topic) {

        return service.declarative(topic);
    }

}
