# MCP Server Template

A general-purpose starting point for building an [MCP (Model Context Protocol)](https://modelcontextprotocol.io/)
server with **Spring Boot + Spring AI**. Unlike a single-purpose demo, this template is intentionally
domain-agnostic — the sample tools it ships with (echo, current time, UUID generator, text counter) exist
only to show the wiring. Swap them out for whatever your project actually needs.

## Project layout

```
src/main/java/com/example/mcp/server/
  McpServerApplication.java     # entry point; registers tool services
  config/StartupConfig.java     # startup banner / usage message
  controller/HealthController.java  # plain REST /health and /info endpoints
  exception/GlobalExceptionHandler.java
  service/SampleToolService.java    # EXAMPLE tools — replace with your own
src/main/resources/
  application.properties
  banner.txt
package.json               # npm scripts to launch MCP Inspector (no other JS deps)
mcp-inspector.config.json  # points Inspector at this server's SSE endpoint
```

## How to add your own tools

1. Create a new `@Service` class (or edit `SampleToolService`).
2. Annotate each method you want to expose with `@Tool(description = "...")`.
   Keep parameter types simple (String, double, boolean, int, etc.) — they
   become the tool's JSON input schema automatically.
3. Register the service in `McpServerApplication#mcpTools(...)` by adding it
   as a parameter and passing it into `.toolObjects(...)`.

```java
@Service
public class WeatherToolService {
    @Tool(description = "Get the current weather for a city")
    public String getWeather(String city) {
        // your implementation
    }
}
```

```java
@Bean
public ToolCallbackProvider mcpTools(SampleToolService sampleToolService,
                                      WeatherToolService weatherToolService) {
    return MethodToolCallbackProvider.builder()
            .toolObjects(sampleToolService, weatherToolService)
            .build();
}
```

You can add as many tool-bearing services as you like — group them by
domain area (e.g. `OrdersToolService`, `InventoryToolService`) rather than
cramming everything into one class.

## How to Run

### Prerequisites

- Java 21+
- Maven (or use the included `./mvnw` wrapper)

### Build & Run

```bash
./mvnw clean package -DskipTests
java -jar target/mcp-server-template-0.1.0-SNAPSHOT.jar
```

Server starts at `http://localhost:8080`.
- `GET /health` — health check
- `GET /info` — service + tool metadata
- MCP tools are served over the Spring AI MCP transport (WebFlux/SSE)

### Docker

```bash
docker build -t mcp-server-template .
docker run -p 8080:8080 mcp-server-template
```

### IDE Configuration

`mcp.json` configures the server for MCP-aware IDEs (e.g. JetBrains: Settings → Tools → MCP).
Update the jar filename inside it if you rename the artifact.

### Testing with MCP Inspector

The [MCP Inspector](https://github.com/modelcontextprotocol/inspector) is the standard tool for
manually testing an MCP server's tools (and, once you add them, prompts/resources) from a browser
UI — no client integration needed. It's a separate Node.js CLI, not a Java dependency, so it isn't
bundled into the Maven build; it's launched alongside your running server.

1. Start the server first:
   ```bash
   ./mvnw spring-boot:run
   ```
   By default the Spring AI WebFlux starter exposes the SSE endpoint at `http://localhost:8080/sse`
   (and the message endpoint at `/mcp/messages`). Confirm/override these via
   `spring.ai.mcp.server.sse-endpoint` and `spring.ai.mcp.server.sse-message-endpoint` in
   `application.properties` if you change the transport config.

2. In another terminal, launch Inspector using the included config (`mcp-inspector.config.json`)
   via the npm script:
   ```bash
   npm run inspector
   ```
   This runs `npx @modelcontextprotocol/inspector --config mcp-inspector.config.json --server mcp-server-template`,
   which connects straight to your running server over SSE and opens the Inspector UI in your browser.

   Alternatively, launch Inspector with no config and connect manually through the UI:
   ```bash
   npm run inspector:ui
   # or: npx @modelcontextprotocol/inspector
   ```
   Then choose transport type "SSE" and enter `http://localhost:8080/sse` as the URL.

3. In the Inspector UI, open the **Tools** tab to see `echo`, `currentTime`, `generateUuid`,
   `countText`, and `help` (or whatever you've replaced them with), inspect their generated input
   schemas, and invoke them with test arguments to see live results.

`package.json` here exists solely to hold these two convenience scripts — this project has no
other Node/JS runtime dependency.

## Customizing the project

- **Rename the artifact**: update `artifactId`, `name`, and `description` in `pom.xml`,
  and the jar filename referenced in `Dockerfile` and `mcp.json`.
- **Change the base package**: rename `com.example.mcp.server` to match your organization,
  updating package declarations and imports across all Java files.
- **Startup messages**: edit `mcp.service.welcome` / `mcp.service.usage` in `application.properties`.
- **Banner**: replace `src/main/resources/banner.txt` with your own ASCII art or delete it.

## Learn more

- [Spring AI MCP docs](https://docs.spring.io/spring-ai/reference/api/mcp.html) — Spring AI MCP support
- [Model Context Protocol specification](https://modelcontextprotocol.io/) — Official docs
- [mcp-for-beginners](https://github.com/microsoft/mcp-for-beginners) — hands-on tutorial for building MCP servers from scratch
