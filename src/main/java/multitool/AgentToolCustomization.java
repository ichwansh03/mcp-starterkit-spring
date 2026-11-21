package multitool;

import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.AgentTool;
import com.google.common.collect.ImmutableList;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AgentToolCustomization {

  private static final String APP_NAME = "summary_agent";
  private static final String USER_ID = "user1234";
  private static final String MODEL_NAME = "gemini-3-flash-preview";

  public static void initAgentAndRun(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("prompt must not be blank");
    }

    LlmAgent summaryAgent =
        LlmAgent.builder()
            .model(MODEL_NAME)
            .name("summaryAgent")
            .instruction(
                "You are an expert summarizer. Please read the following text and provide a concise summary.")
            .description("Agent to summarize text")
            .build();

    // Define root_agent
    LlmAgent rootAgent =
        LlmAgent.builder()
            .model(MODEL_NAME)
            .name("rootAgent")
            .instruction(
                "You are a helpful assistant. When the user provides a text, always use the 'summaryAgent' tool to generate a summary. Always forward the user's message exactly as received to the 'summaryAgent' tool, without modifying or summarizing it yourself. Present the response from the tool to the user.")
            .description("Assistant agent")
            .tools(AgentTool.create(summaryAgent, true)) // Set skipSummarization to true
            .build();

    // Create an InMemoryRunner
    InMemoryRunner runner = new InMemoryRunner(rootAgent, APP_NAME);
    // InMemoryRunner automatically creates a session service. Create a session using the service
    Session session = runner.sessionService().createSession(APP_NAME, USER_ID).blockingGet();
    Content userMessage = Content.fromParts(Part.fromText(prompt));

    // Run the agent
    Flowable<Event> eventStream = runner.runAsync(USER_ID, session.id(), userMessage);

    // Stream event response
    AtomicBoolean printedFinalResponse = new AtomicBoolean(false);
    eventStream.blockingForEach(
        event -> {
          if (event.finalResponse()) {
            printedFinalResponse.set(true);
            printFinalResponse(event);
          }
        });

    if (!printedFinalResponse.get()) {
      System.out.println("WARN: no final response event was produced.");
    }
  }

  /**
   * With {@code skipSummarization = true}, the final event carries the sub-agent result as a
   * function response instead of plain text. Prefer the readable text, then the tool result, and
   * fall back to the raw event.
   */
  private static void printFinalResponse(Event event) {
    List<Part> parts =
        event.content().flatMap(Content::parts).orElse(ImmutableList.of());
    String text =
        parts.stream()
            .map(Part::text)
            .flatMap(Optional::stream)
            .filter(s -> !s.isBlank())
            .collect(Collectors.joining(" "));
    if (!text.isEmpty()) {
      System.out.println(text);
      return;
    }

    String toolResult =
        parts.stream()
            .map(Part::functionResponse)
            .flatMap(Optional::stream)
            .map(fr -> fr.response().map(response -> response.get("result")).orElse(null))
            .filter(value -> value != null && !value.toString().isBlank())
            .map(Object::toString)
            .findFirst()
            .orElse("");
    System.out.println(toolResult.isEmpty() ? event.stringifyContent() : toolResult);
  }

  public static void main(String[] args) {
    String longText =
        """
            Quantum computing represents a fundamentally different approach to computation,
            leveraging the bizarre principles of quantum mechanics to process information. Unlike classical computers
            that rely on bits representing either 0 or 1, quantum computers use qubits which can exist in a state of superposition - effectively
            being 0, 1, or a combination of both simultaneously. Furthermore, qubits can become entangled,
            meaning their fates are intertwined regardless of distance, allowing for complex correlations. This parallelism and
            interconnectedness grant quantum computers the potential to solve specific types of incredibly complex problems - such
            as drug discovery, materials science, complex system optimization, and breaking certain types of cryptography - far
            faster than even the most powerful classical supercomputers could ever achieve, although the technology is still largely in its developmental stages.""";

    initAgentAndRun(longText);
  }
}