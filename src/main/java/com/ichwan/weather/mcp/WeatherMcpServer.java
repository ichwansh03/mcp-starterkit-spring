package com.ichwan.weather.mcp;

import com.ichwan.weather.mcp.controller.WeatherToolController;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WeatherMcpServer {

	private static final String WEATHER_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "city": { "type": "string", "description": "Nama kota, misal 'Jakarta'" }
              },
              "required": ["city"]
            }
            """;

	public static void main(String[] args) {
		var jsonMapper = McpJsonDefaults.getMapper();
		StdioServerTransportProvider transportProvider =
				new StdioServerTransportProvider(jsonMapper);

		WeatherToolController controller = new WeatherToolController();

		McpServerFeatures.SyncToolSpecification weatherTool = McpServerFeatures.SyncToolSpecification.builder()
				.tool(McpSchema.Tool.builder()
						.name("get_current_weather")
						.description("Ambil cuaca terkini untuk sebuah kota")
						.inputSchema(jsonMapper, WEATHER_SCHEMA)
						.build())
				.callHandler((exchange, request) -> controller.getCurrentWeather(request))
				.build();

		McpSyncServer server = McpServer.sync(transportProvider)
				.serverInfo("weather-server", "1.0.0")
				.capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
				.tools(weatherTool)
				.build();

		Runtime.getRuntime().addShutdownHook(new Thread(server::close));
	}

}
