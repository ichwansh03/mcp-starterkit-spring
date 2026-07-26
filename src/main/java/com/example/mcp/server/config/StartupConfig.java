package com.example.mcp.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Displays welcome and usage information at application startup.
 * Customize the messages via application.properties.
 */
@Configuration
public class StartupConfig {

    @Value("${mcp.service.welcome:Welcome to the MCP Server Template!}")
    private String welcomeMessage;

    @Value("${mcp.service.usage:}")
    private String usageMessage;

    @Bean
    public CommandLineRunner startupInfo() {
        return args -> {
            System.out.println("\n" + "=".repeat(80));
            System.out.println(welcomeMessage);
            System.out.println("=".repeat(80));

            if (usageMessage != null && !usageMessage.isEmpty()) {
                System.out.println("\nUsage Information:");
                System.out.println(usageMessage);
            }

            System.out.println("\nHealth check:   http://localhost:8080/health");
            System.out.println("Service info:   http://localhost:8080/info");
            System.out.println("\nSee the README.md for more information on how to use this template.");
            System.out.println("\nThe MCP server is now ready to accept requests!");
            System.out.println("=".repeat(80) + "\n");
        };
    }
}
