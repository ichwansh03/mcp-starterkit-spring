package com.example.mcp.server;

import com.example.mcp.server.service.SampleToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Entry point for this MCP (Model Context Protocol) server.
 *
 * This is a general-purpose template: it is not tied to any specific domain.
 * Register your own @Service classes (annotated with @Tool methods, see
 * {@link SampleToolService} for an example) as tool objects below, and the
 * MethodToolCallbackProvider will expose them as MCP tools automatically.
 *
 * Run with the MCP Inspector for manual testing:
 *   npx @modelcontextprotocol/inspector
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    /**
     * Registers all tool-bearing services with the MCP server.
     * Add additional services as constructor/method parameters and pass
     * them into toolObjects(...) to expose more tools.
     */
    @Bean
    public ToolCallbackProvider mcpTools(SampleToolService sampleToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(sampleToolService)
                .build();
    }
}
