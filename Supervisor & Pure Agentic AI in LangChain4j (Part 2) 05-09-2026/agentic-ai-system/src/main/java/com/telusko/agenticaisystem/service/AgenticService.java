package com.telusko.agenticaisystem.service;

import com.telusko.agenticaisystem.agents.Agents;
import com.telusko.agenticaisystem.agents.Agents.*;
import com.telusko.agenticaisystem.agents.Intent;
import com.telusko.agenticaisystem.agents.Review;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.agent.ErrorRecoveryResult;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class AgenticService
{
    private ChatModel chatModel;
    private StreamingChatModel streamingChatModel;

    public AgenticService(ChatModel chatModel, StreamingChatModel streamingChatModel)
    {
        this.chatModel=chatModel;
        this.streamingChatModel=streamingChatModel;
    }

    public String basicAgent(String topic)
    {
        StoryWriter writer = AgenticServices.agentBuilder(StoryWriter.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        return writer.write(topic);
    }

    public String sequential(String topic, String audience)
    {
        //first agent --> create story
       StoryWriter writer = AgenticServices.agentBuilder(StoryWriter.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        //second agent --> modfies story --> target audience
       AudienceEditor audienceEditor = AgenticServices.agentBuilder(AudienceEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

        //third agent
        StyleEditor styleEditor = AgenticServices.agentBuilder(StyleEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();

//        System.out.println("----------------------------");
//        HashMap<String, Object> input = new HashMap<>();
//        input.put("topic", topic);
//        input.put("audience", audience);
//
//        System.out.println("Agent 1 --> Story writer --> 1");
//        writer.write(input);




        //sequential workflow
        UntypedAgent pipeline = AgenticServices.sequenceBuilder()
                .subAgents(
                        writer,
                        audienceEditor,
                        styleEditor
                )
                .outputKey("story")
                .build();

            return (String) pipeline.invoke(Map.of(
                    "topic", topic,
                    "audience", audience
            ));
    }
    public String loop(String story)
    {
        // Agent responsible for evaluating the story.
        StyleScorer scorer =
                AgenticServices.agentBuilder(StyleScorer.class)
                        .chatModel(chatModel)

                        // The scorer returns a Review object.
                        // Store it under "review".
                        .outputKey("review")
                        .build();
        Review res = scorer.score(story);
        System.out.println("Score :"+res.score());
        System.out.println("Feedback : "+ res.feedback());


        // Agent responsible for improving the story.
        StyleImprover improver =
                AgenticServices.agentBuilder(StyleImprover.class)
                        .chatModel(chatModel)

                        // The improved story becomes the
                        // current value of "story".
                        .outputKey("story")
                        .build();

        UntypedAgent refiner = AgenticServices.loopBuilder()
                .subAgents(scorer,
                        improver)
                .outputKey("story")
                .maxIterations(2)
                .exitCondition(scope -> ((Review) scope.readState("review"))
                        .score() >= 0.8)
                .build();

        return (String) refiner.invoke(
                Map.of("story", story)
        );
    }
    public Review parallelAgents(String text)
    {
        // Create SEO review agent.
        SeoReviewer seo =
                AgenticServices.agentBuilder(SeoReviewer.class)
                        .chatModel(chatModel)
                        .outputKey("seoReview")
                        .build();


        // Create readability review agent.
        ReadabilityReviewer readability =
                AgenticServices.agentBuilder(ReadabilityReviewer.class)
                        .chatModel(chatModel)
                        .outputKey("readabilityReview")
                        .build();

        var executor=Executors.newFixedThreadPool(2);

        UntypedAgent pipeline = AgenticServices.parallelBuilder()
                .subAgents(
                        seo,
                        readability
                )
                .executor(executor)
                .outputKey("finalReview")
                .output(
                        agenticScope -> {
                            Review a = (Review) agenticScope.readState("seoReview");

                            Review b = (Review) agenticScope.readState("readabilityReview");

                            return new Review(
                                    a.score() + b.score() / 2.0,
                                    "SEO: " + a.feedback() + " | Readability: " + b.feedback()
                            );
                        })
                .build();

//       return (Review) pipeline.invoke(
//                Map.of("story", text));
        Review result=(Review) pipeline.invoke(Map.of("story", text));
        executor.shutdown();
        return result;
    }
    public Object mapper(List<String> topics)
    {
        TopicSummarizer summarizer =
                AgenticServices.agentBuilder(TopicSummarizer.class)
                        .chatModel(chatModel)
                        .outputKey("summary")
                        .build();

        var executor=Executors.newFixedThreadPool(4);
        UntypedAgent batch = AgenticServices.parallelMapperBuilder()
                .subAgents(summarizer)
                .itemsProvider("topics")
                .outputKey("summaries")
                .executor(executor)
                .build();
        Object result = batch.invoke(
                Map.of("topics", topics)
        );
        executor.shutdown();
        return result;
    }
    public String condionalAgents(String message)
    {
        // First create the classifier agent.
        Classifier classifier =
                AgenticServices.agentBuilder(Classifier.class)
                        .chatModel(chatModel)

                        // Classifier returns an Intent.
                        .outputKey("intent")
                        .build();
      String res=classifier.classify(message).toString();
        System.out.println(res);


        // Agent used when intent is QUESTION.
        QuestionResponder question =
                AgenticServices.agentBuilder(QuestionResponder.class)
                        .chatModel(chatModel)
                        .outputKey("answer")
                        .build();

        String reply=question.reply(message);
        System.out.println(reply);


        // Agent used when intent is COMPLAINT.
        ComplaintResponder complaint =
                AgenticServices.agentBuilder(ComplaintResponder.class)
                        .chatModel(chatModel)
                        .outputKey("answer")
                        .build();


        // Agent used when intent is PRAISE.
        PraiseResponder praise =
                AgenticServices.agentBuilder(PraiseResponder.class)
                        .chatModel(chatModel)
                        .outputKey("answer")
                        .build();
        UntypedAgent pipeline =
                AgenticServices.conditionalBuilder()
                .subAgents(
                        agenticScope -> agenticScope.readState("intent") == Intent.QUESTION, question
                )
                .subAgents(
                        agenticScope -> agenticScope.readState("intent") == Intent.COMPLAINT, complaint

                ).subAgents(
                        agenticScope -> agenticScope.readState("intent") == Intent.PRAISE, praise

                )
                .build();
        UntypedAgent finalPipeline = AgenticServices.sequenceBuilder()
                .subAgents(classifier, pipeline)
                .outputKey("answer")

                .build();
        return (String) finalPipeline.invoke(Map.of(
                "message", message
                )
        );
    }
    public Object optionalAsync(String topic)
    {
        StoryWriter draft =
                AgenticServices.agentBuilder(StoryWriter.class)
                        .chatModel(chatModel)
                        .outputKey("story")
                        .build();

        Translator translator = AgenticServices.agentBuilder(Translator.class)
                .chatModel(chatModel)
                .outputKey("french")
                .optional(true)
                .build();


        FactChecker factChecker = AgenticServices.agentBuilder(FactChecker.class)
                .chatModel(chatModel)
                .outputKey("factNote")
                .async(true)
                .build();

        UntypedAgent pipeline = AgenticServices.sequenceBuilder()
                .subAgents(
                        draft,
                        translator,
                        factChecker
                )
                .outputKey("story")
                .build();

        return pipeline.invoke(
                Map.of(
                        "topic", topic
                ));
    }

    public String streaming(String topic)
    {
        StringBuilder assembled=new StringBuilder();
        CompletableFuture<String> done =
                new CompletableFuture<>();

        streamingChatModel.chat(
                "Write a short story about " + topic + " .",
                new StreamingChatResponseHandler()
                {
                    @Override
                    public void onPartialResponse(String token)
                    {
                        assembled.append(token);
                        System.out.println(token);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse chatResponse) {
                        done.complete(assembled.toString());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        done.completeExceptionally(throwable);
                    }
                }
        );
        return done.join();
    }

    public String errorRecovery() {

        // Create the StoryWriter agent.
        StoryWriter writer =
                AgenticServices.agentBuilder(StoryWriter.class)
                        .chatModel(chatModel)
                        .outputKey("story")
                        .build();


        // Create a workflow containing the writer.
        UntypedAgent safeWriter =
                AgenticServices.sequenceBuilder()

                        .subAgents(writer)

                        .outputKey("story")

                        // Define what should happen if
                        // an agent execution fails.
                        .errorHandler(ctx -> {

                            // Check whether "topic" exists
                            // in the shared AgenticScope.
                            if (ctx.agenticScope()
                                    .readState("topic") == null) {

                                // Topic is missing.
                                //
                                // Add a default topic so the
                                // StoryWriter has something to use.
                                ctx.agenticScope()
                                        .writeState(
                                                "topic",
                                                "a friendly dragon"
                                        );


                                // Tell LangChain4j:
                                // "Now try the failed operation again."
                                return ErrorRecoveryResult.retry();
                            }


                            // If the problem is something other
                            // than the missing topic,
                            // don't try to recover automatically.
                            return ErrorRecoveryResult
                                    .throwException();
                        })

                        .build();


        // Intentionally invoke without any input.
        //
        // This allows us to demonstrate the recovery mechanism.
        return (String) safeWriter.invoke(
                Map.of()
        );
    }
    public String observability(String topic) {
        AgentListener logger =
                new AgentListener(){

                    @Override
                    public void beforeAgentInvocation(
                            AgentRequest request) {

                        // This method runs BEFORE the agent executes.
                        //
                        // Print which agent is being called
                        // and what input it received.
                        System.out.println(
                                ">> calling "
                                        + request.agentName()
                                        + " with "
                                        + request.inputs()
                        );
                    }


                    @Override
                    public void afterAgentInvocation(
                            AgentResponse response) {

                        // This method runs AFTER the agent finishes.
                        //
                        // Print the agent name and its result.
                        System.out.println(
                                "<< "
                                        + response.agentName()
                                        + " returned: "
                                        + response.output()
                        );
                    }
                };

        StoryWriter writer =
                AgenticServices.agentBuilder(StoryWriter.class)
                        .chatModel(chatModel)
                        .outputKey("story")

                        // Attach our listener to this agent.
                        .listener(logger)

                        .build();
return writer.write(topic);
    }
    public String human(
            String request,
            String humanDecision) {

        // Create the AI agent that proposes a decision.
        DecisionProposer proposer =
                AgenticServices.agentBuilder(
                                DecisionProposer.class)
                        .chatModel(chatModel)
                        // Save AI proposal under "proposal".
                        .outputKey("proposal")
                        .build();


        HumanInTheLoop approval = AgenticServices.humanInTheLoopBuilder()
                .description(
                        " A human approves or changes the proposed decision"
                )
                .outputKey("finalDecision")
                .responseProvider(scope -> {

                    // Display the AI's proposal.
                    System.out.println(
                            "Assistant proposed: "
                                    + scope.readState("proposal")
                    );
                    return humanDecision;
                })
                .build();

        UntypedAgent workflow =
                AgenticServices.sequenceBuilder()

                        .subAgents(
                                proposer,
                                approval
                        )

                        .outputKey("finalDecision")

                        .build();
        return (String) workflow.invoke(
                Map.of("request", request)
        );
    }
    public String nonAi(String topic) {

        // First create an AI StoryWriter agent.
        StoryWriter writer =
                AgenticServices.agentBuilder(StoryWriter.class)
                        .chatModel(chatModel)
                        .outputKey("story")
                        .build();

        UntypedAgent pipeline = AgenticServices
                .sequenceBuilder()
                .subAgents(
                        writer,
                        new WordCounter(),
                        AgenticServices.agentAction(
                                scope -> {

                                    // Read the word count
                                    // produced by WordCounter.
                                    int count =
                                            (int) scope.readState(
                                                    "wordCount"
                                            );


                                    // Based on the word count,
                                    // create a simple label.
                                    //
                                    // More than 40 words → long
                                    // Otherwise → short
                                    scope.writeState(
                                            "lengthLabel",

                                            count > 40
                                                    ? "long"
                                                    : "short"
                                    );
                                }
                        )
                )
                .outputKey("lengthLabel")
                .build();
        return (String)pipeline.invoke(
                Map.of("topic", topic)
        );

    }

    public String declarative(String topic)
    {
        BlogCreator creator =
                AgenticServices.createAgenticSystem(
                        BlogCreator.class,

                        // Tell the declarative system
                        // which LLM should be used.
                        chatModel
                );
        return creator.create(topic);
    }
}




