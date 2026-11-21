package multitool;

import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.LongRunningFunctionTool;
import com.google.adk.tools.ToolContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.Part;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Long-running tool example.
 *
 * <p>The tool only starts an asynchronous operation. The application reports progress and the
 * final result in later turns by sending {@code FunctionResponse} parts with the same call id.
 */
public class LongRunningFunctionExample {

  private static final String APP_NAME = "ticket_app";
  private static final String USER_ID = "user123";
  private static final String AGENT_NAME = "ticket_agent";
  private static final String MODEL_NAME = "gemini-3-flash-preview";
  private static final String TOOL_NAME = "create_ticket_long_running";

  @Schema(
      name = TOOL_NAME,
      description =
          """
          Creates a new support ticket with a specified urgency level.
          Examples of urgency are 'high', 'medium', or 'low'.
          The ticket creation is a long-running process; the ticket ID and status
          are delivered asynchronously by the application.
          """)
  public static void createTicketAsync(
      @Schema(
              name = "urgency",
              description =
                  "The urgency level for the new ticket, such as 'high', 'medium', or 'low'.")
          String urgency,
      @Schema(name = "toolContext") // Ensures ADK injection
          ToolContext toolContext) {
    System.out.printf(
        "%n[Tool] %s started with urgency='%s' (call id: %s)%n",
        TOOL_NAME, urgency, toolContext.functionCallId().orElse("N/A"));
    System.out.println("[Tool] Ticket is being created asynchronously.");
  }

  public static void main(String[] args) {
    LlmAgent agent =
        LlmAgent.builder()
            .name(AGENT_NAME)
            .description("Agent for creating support tickets via a long-running task.")
            .model(MODEL_NAME)
            .instruction(
                "You create support tickets. When the user asks for a ticket, call the"
                    + " 'create_ticket_long_running' tool with the urgency level. Ticket creation"
                    + " is asynchronous: never claim that a ticket exists before the application"
                    + " sends its ticket_id and status. Once the application reports the final"
                    + " status, summarize the result for the user.")
            .tools(
                ImmutableList.of(
                    LongRunningFunctionTool.create(
                        LongRunningFunctionExample.class, "createTicketAsync")))
            .build();

    Runner runner = new InMemoryRunner(agent, APP_NAME);
    Session session = runner.sessionService().createSession(APP_NAME, USER_ID).blockingGet();

    // --- Turn 1: user requests a ticket; the model calls the long-running tool ---
    System.out.println("--- Turn 1: user request ---");
    Content initialUserMessage =
        Content.fromParts(Part.fromText("Create a high urgency ticket for me, please."));

    AtomicReference<String> callIdRef = new AtomicReference<>();
    runner
        .runAsync(USER_ID, session.id(), initialUserMessage)
        .blockingForEach(
            event -> {
              printEventSummary(event, "T1");
              if (callIdRef.get() == null) {
                // Capture the first matching function call id; the application needs it to
                // complete the tool later.
                event.content().flatMap(Content::parts).orElse(ImmutableList.of()).stream()
                    .map(Part::functionCall)
                    .flatMap(Optional::stream)
                    .filter(fc -> TOOL_NAME.equals(fc.name().orElse("")))
                    .findFirst()
                    .flatMap(fc -> fc.id())
                    .ifPresent(callIdRef::set);
              }
            });

    if (callIdRef.get() == null) {
      System.out.println("ERROR: tool '" + TOOL_NAME + "' was not called in turn 1.");
      return;
    }
    System.out.println("[App] Captured long-running call id: " + callIdRef.get());

    // --- Turn 2: the application reports the created (still pending) ticket ---
    System.out.println("\n--- Turn 2: application reports the ticket id ---");
    String ticketId = "TICKET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    sendToolResponse(
        runner,
        session,
        callIdRef.get(),
        ImmutableMap.of("ticket_id", ticketId, "status", "pending"),
        "T2");
    System.out.println("[App] Sent ticket_id " + ticketId + " (pending) to the agent.");

    // --- Turn 3: the application reports the final status ---
    System.out.println("\n--- Turn 3: application reports the final status ---");
    sendToolResponse(
        runner,
        session,
        callIdRef.get(),
        ImmutableMap.of("ticket_id", ticketId, "status", "approved"),
        "T3");
    System.out.println("\nLong-running ticket flow completed.");
  }

  private static void sendToolResponse(
      Runner runner,
      Session session,
      String functionCallId,
      Map<String, Object> response,
      String turnLabel) {
    FunctionResponse functionResponse =
        FunctionResponse.builder()
            .name(TOOL_NAME)
            .id(functionCallId)
            .response(response)
            .build();
    Content appResponse =
        Content.builder()
            .role("user")
            .parts(ImmutableList.of(Part.builder().functionResponse(functionResponse).build()))
            .build();

    runner
        .runAsync(USER_ID, session.id(), appResponse)
        .blockingForEach(event -> printEventSummary(event, turnLabel));
  }

  private static void printEventSummary(Event event, String turnLabel) {
    event
        .content()
        .ifPresent(
            content -> {
              String text =
                  content.parts().orElse(ImmutableList.of()).stream()
                      .map(part -> part.text().orElse(""))
                      .filter(s -> !s.isEmpty())
                      .collect(Collectors.joining(" "));
              if (!text.isEmpty()) {
                System.out.printf("[%s][%s_TEXT]: %s%n", turnLabel, event.author(), text);
              }

              content.parts().orElse(ImmutableList.of()).stream()
                  .map(Part::functionCall)
                  .flatMap(Optional::stream)
                  .findFirst() // One relevant function call per event keeps the sample simple.
                  .ifPresent(
                      fc ->
                          System.out.printf(
                              "[%s][%s_CALL]: %s(%s) id=%s%n",
                              turnLabel,
                              event.author(),
                              fc.name().orElse("N/A"),
                              fc.args().orElse(ImmutableMap.of()),
                              fc.id().orElse("N/A")));

              content.parts().orElse(ImmutableList.of()).stream()
                  .map(Part::functionResponse)
                  .flatMap(Optional::stream)
                  .findFirst()
                  .ifPresent(
                      fr ->
                          System.out.printf(
                              "[%s][%s_RESPONSE]: %s -> %s%n",
                              turnLabel, event.author(), fr.name().orElse("N/A"), fr.response()));
            });
  }
}
