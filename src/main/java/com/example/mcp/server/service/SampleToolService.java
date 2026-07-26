package com.example.mcp.server.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Example service exposing a handful of generic, domain-agnostic tools.
 *
 * This class exists purely as a TEMPLATE / reference implementation showing
 * how to define MCP tools with Spring AI:
 *   - annotate the class with @Service so Spring can manage it
 *   - annotate each public method you want to expose with @Tool(description = "...")
 *   - keep parameter names and types simple; they become the tool's input schema
 *
 * Replace or extend this class with services relevant to your own domain
 * (e.g. inventory lookups, ticket search, weather data, database queries...).
 * You can add as many @Service tool classes as you like — just remember to
 * register each one as a tool object in McpServerApplication.
 */
@Service
public class SampleToolService {

    /**
     * Simple echo tool - useful for verifying connectivity end-to-end.
     */
    @Tool(description = "Echo back the given message, optionally uppercased")
    public String echo(String message, boolean uppercase) {
        if (message == null) {
            return "Error: message must not be null";
        }
        return uppercase ? message.toUpperCase() : message;
    }

    /**
     * Returns the current server time in ISO-8601 format for a given IANA time zone.
     */
    @Tool(description = "Get the current server time for a given IANA time zone id (e.g. 'UTC', 'America/New_York')")
    public String currentTime(String timeZoneId) {
        try {
            ZoneId zone = (timeZoneId == null || timeZoneId.isBlank())
                    ? ZoneId.of("UTC")
                    : ZoneId.of(timeZoneId);
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(zone));
        } catch (Exception e) {
            return "Error: unrecognized time zone id '" + timeZoneId + "'";
        }
    }

    /**
     * Generates a random UUID - handy placeholder for ID-generation style tools.
     */
    @Tool(description = "Generate a random UUID (v4)")
    public String generateUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Counts words and characters in a piece of text.
     */
    @Tool(description = "Count words and characters in the given text")
    public String countText(String text) {
        if (text == null) {
            return "Error: text must not be null";
        }
        String trimmed = text.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        int chars = text.length();
        return String.format("words=%d, characters=%d", words, chars);
    }

    /**
     * Lists the tools available in this service, along with a short description.
     * Update this list whenever you add or remove tools.
     */
    @Tool(description = "Get help describing the tools available in this MCP server")
    public String help() {
        return "MCP Server Template - Available tools:\n\n" +
                "1. echo(message, uppercase) - Echoes a message back, optionally uppercased\n" +
                "2. currentTime(timeZoneId) - Returns the current time for a time zone\n" +
                "3. generateUuid() - Generates a random UUID\n" +
                "4. countText(text) - Counts words and characters in text\n" +
                "5. help() - Shows this message\n\n" +
                "This is a template project. Replace these sample tools with your own " +
                "domain-specific @Service classes annotated with @Tool.";
    }
}
