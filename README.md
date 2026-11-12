# Google ADK Java Agents

A small collection of [Google ADK](https://github.com/google/adk-java) agents written in Java 21.
The project demonstrates four building blocks: a minimal function-calling agent, a multi-tool agent
with structured JSON output, a built-in code-execution agent, and a plain CLI runner.

All agents use Gemini models through the Google AI Studio API key.

## Requirements

- Java 21+
- Maven 3.9+
- A Gemini API key (`GEMINI_API_KEY` or `GOOGLE_API_KEY`)

## Configuration

Create a `.env` file in the project root:

```bash
export GEMINI_API_KEY="your-api-key"
```

`.env` is git-ignored. The key is read by the Google Gen AI SDK from the process environment, so it
must be exported **before** the JVM starts. Sourcing `.env` inside a script (as `run.sh` does) is
enough because `exec:java` runs in the same JVM as Maven.

## Build

```bash
mvn -q -DskipTests compile
```

## Agents

### `HelloTimeAgent` — `hello-time-agent`

A minimal `LlmAgent` that exposes one `FunctionTool`, `getCurrentTime`.

- **Tools:** `getCurrentTime(city)`
- **Behavior:** returns a fixed, mocked answer (`The time is 10:30am.`). The `city` argument is
  accepted by the tool signature but is not used by the implementation.
- **Purpose:** smallest possible example of tool declaration, `@Schema` parameter annotations, and
  running an agent through `InMemoryRunner`.
- **Entry point:** `AgentCliRunner` (see below).

### `MultiToolAgent` — `multi_tool_agent`

A two-tool agent that answers time and weather questions.

- **Tools:**
  - `getCurrentTime(city)` — normalizes the city name and looks up a matching `ZoneId`, returning
    the current local time.
  - `getWeather(city)` — mocked weather lookup (only `Jakarta` returns a success result).
- **Structured output:** the agent declares an `outputSchema` (`status`, `report`, both required)
  and an `outputKey("multi_tool_result")`.
  - The schema becomes `responseSchema` + `responseMimeType="application/json"` on the Gemini
    request.
  - The validated result is written to session state under `multi_tool_result`; invalid JSON is
    stored as a raw string with a warning.
  - With Gemini 3 models the schema is applied natively and can be combined with tools. With
    Gemini 2.x, ADK instead injects a synthetic `set_model_response` tool, which is why mixing
    tools and structured output is discouraged on those models.
- **Entry point:** available in the dev UI and in its own `main` CLI.

### `CodeExecutionAgentApp` — `calculator_agent`

An agent that uses Gemini's built-in Python code execution tool.

- **Tools:** `BuiltInCodeExecutionTool`
- **Model:** `gemini-3-flash-preview`
- **Behavior:** writes and executes Python to evaluate expressions, then returns the numeric result
  as plain text. For example, `(5 + 7) * 3` produces `36` and `10!` produces `3628800`.
- **Dev UI:** exposes `public static final BaseAgent ROOT_AGENT`, so the ADK web server discovers it
  automatically.
- **Entry point:** its own `main` runs two sample queries through `Runner`; the same agent is also
  served by the dev UI.

### `AgentCliRunner`

A command-line harness for `HelloTimeAgent` built on `InMemoryRunner`: creates a session, reads user input from stdin, streams events, and prints the final response. Type `quit`
to exit.

It is a runner, not an agent: it contains no instructions, tools, or model configuration.

## Running the agents

### Dev UI (all discoverable agents)

```bash
./run.sh
```

The script sources `.env`, then starts `com.google.adk.web.AdkWebServer` with
`--adk.agents.source-dir=target --server.port=8080`. Open <http://localhost:8080>.

The loader scans compiled classes for a `public static final BaseAgent ROOT_AGENT` field, so the
available apps are `hello-time-agent`, `multi_tool_agent`, and `calculator_agent`.

Notes:

- `run.sh` is blocking. Do not start it twice; the second start fails with
  `Port 8080 was already in use`.
- Recompile and restart after changing an agent; the loader reads `target/classes` at startup.
- Stop with `Ctrl-C`, or `kill` the PID shown by
  `lsof -nP -iTCP:8080 -sTCP:LISTEN`.

### CLI: hello-time agent

```bash
set -a; . ./.env; set +a
mvn -q compile exec:java -Dexec.mainClass=AgentCliRunner
```

### CLI: multi-tool agent

```bash
set -a; . ./.env; set +a
mvn -q compile exec:java -Dexec.mainClass=multitool.MultiToolAgent
```

### CLI: code-execution agent

```bash
set -a; . ./.env; set +a
mvn -q compile exec:java -Dexec.mainClass=CodeExecutionAgentApp
```

## Project layout

```
src/main/java/
  AgentCliRunner.java          # CLI harness for HelloTimeAgent
  HelloTimeAgent.java          # minimal single-tool agent
  CodeExecutionAgentApp.java   # built-in code execution agent, exposed to the dev UI
  multitool/
    MultiToolAgent.java        # time + weather tools, structured JSON output
pom.xml                        # google-adk and google-adk-dev 1.12.0
run.sh                         # loads .env and starts the ADK dev UI on port 8080
```

## Models and quotas

The examples use `gemini-3-flash-preview`. Free-tier quota for this model is limited (for example,
20 generate-content requests per day in some projects), and the API returns
`429 RESOURCE_EXHAUSTED` once it is spent. Other models, such as `gemini-2.0-flash`, may be retired
and return `404 NOT_FOUND`.

If a request fails, check the exception in the terminal or dev UI log:

- `429` — quota exhausted; wait for the reset or use billing/another model.
- `404` — model retired; switch to a currently available model.
- `Port 8080 was already in use` — another dev-server instance is still running.

## License

No license file is included in this branch.
