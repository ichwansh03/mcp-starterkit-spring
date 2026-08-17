# weather-mcp

Spring Boot **MCP server** exposing weather data as an MCP tool. LLM clients (OpenCode, MCP Inspector, Claude Desktop…) connect over the Model Context Protocol and call `get_current_weather` — the server fetches live data from [Open-Meteo](https://open-meteo.com) (geocoding + current weather), no API key needed.

```
┌─────────────────┐   MCP (JSON-RPC / stdio)   ┌────────────────────┐   REST   ┌───────────┐
│  LLM client     │ ─────────────────────────▶ │  weather-mcp (jar) │ ───────▶ │ Open-Meteo│
│  (opencode,     │ ◀───────────────────────── │  get_current_weather│ ◀─────── │           │
│  Inspector)     │        tool result          └────────────────────┘          └───────────┘
└─────────────────┘
```

## Requirements

| Tool  | Version   | Needed for                   |
| ----- | --------- | ---------------------------- |
| Java  | 21+       | running the server           |
| Node  | >= 22.19  | MCP Inspector only           |
| Maven | —         | via bundled `./mvnw` wrapper |

## Build

```bash
./mvnw -DskipTests package
```

Produces executable jar: `target/weather-mcp-0.0.1.jar`.

## Run manually (stdio server)

```bash
java -jar target/weather-mcp-0.0.1.jar --spring.main.web-application-type=none
```

- `--spring.main.web-application-type=none` disables Tomcat — not needed, MCP speaks stdio only.
- **stdout is reserved for JSON-RPC.** All logging goes to stderr (logback default). Don't print to stdout outside the MCP SDK.
- Kill with Ctrl+C when done (no clean EOF exit).

## Test with MCP Inspector

Web UI debugging tool for any MCP server. Start it pointing at this server:

```bash
# requires Node >= 22.19.0
npx -y @modelcontextprotocol/inspector -- java -jar target/weather-mcp-0.0.1.jar --spring.main.web-application-type=none
```

Then open **http://127.0.0.1:6274** and:

1. **Transport/Command** — server already wired (stdio), click **Connect**.
2. **Tools tab** — `get_current_weather` listed with `city` schema.
3. Fill `city` = `Jakarta` → **Run** → see result JSON-RPC content.
4. **Protocol / Network tabs** — inspect `tools/list`, `tools/call`, raw JSON-RPC frames, timing.

> Inspector v2 syntax: everything after `--` is the spawned command. `MCP_CATALOG_PATH` exported in the shell conflicts with ad-hoc targets — unset it if connection fails.

## Test with OpenCode

Project ships `opencode.json` registering the server:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "weather": {
      "type": "local",
      "command": ["java", "-jar", "target/weather-mcp-0.0.1.jar", "--spring.main.web-application-type=none"],
      "enabled": true
    }
  }
}
```

Steps:

1. Build the jar first (see above). Rebuild after any code change — the jar is stale otherwise.
2. Start opencode from the project root.
3. **Config is loaded once at startup** — after touching `opencode.json`, quit and restart opencode.
4. Verify the connection with `/mcp` in the opencode TUI — the `weather` server should show as connected with 1 tool. (Server logs show `Client initialize request …`.)
5. Ask in chat, e.g. **"cuaca di Jakarta hari ini?"** — the model should call `weather_get_current_weather`, then summarize the result:

   ```
   Cuaca di Jakarta: 26.0°C, angin 3.8 km/jam, kondisi: Berawan sebagian
   ```

If the model never calls the tool: confirm `/mcp` shows `weather` connected, and that your configured model supports tool calling.

## Project layout

```
src/main/java/com/ichwan/weather/mcp/
├── WeatherMcpServer.java          # MCP server bootstrap: stdio transport, tool registration
├── controller/WeatherToolController.java  # tools/call handler → service chain
├── service/GeocodingService.java  # city → coordinates (Open-Meteo geocoding)
├── service/WeatherService.java    # coords → current weather (Open-Meteo)
├── model/                         # Coordinates, CurrentWeather DTOs
└── view/WeatherView.java          # formats result as text (Indonesian)
```

## Troubleshooting

| Symptom | Fix |
| ------- | --- |
| Server boots but tool never appears | Check `/mcp` / Inspector connection; verify jar rebuilt; restart client after config change |
| `SLF4J providers` warning at boot | Gone since `slf4j-simple` was removed — logback only. Rebuild jar if you still see it |
| Port 8080 already in use | Keep `--spring.main.web-application-type=none` — no web server starts |
| Inspector says catalog/ad-hoc conflict | `unset MCP_CATALOG_PATH` before running |