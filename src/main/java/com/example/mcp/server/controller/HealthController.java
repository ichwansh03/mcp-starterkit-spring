package com.example.mcp.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.mcp.server.service.SampleToolService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Plain REST endpoints for health checks and basic service info.
 * Useful for container orchestrators / load balancers, separate from the MCP protocol itself.
 */
@RestController
public class HealthController {

    private final SampleToolService sampleToolService;

    public HealthController(SampleToolService sampleToolService) {
        this.sampleToolService = sampleToolService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "MCP Server Template");
        response.put("toolService", sampleToolService != null ? "Available" : "Unavailable");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> serviceInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "MCP Server Template");
        response.put("version", "0.1.0");
        response.put("description", "General-purpose starting point for building an MCP server with Spring AI");

        Map<String, String> tools = new HashMap<>();
        tools.put("echo", "Echo back the given message, optionally uppercased");
        tools.put("currentTime", "Get the current server time for a given time zone");
        tools.put("generateUuid", "Generate a random UUID");
        tools.put("countText", "Count words and characters in text");
        tools.put("help", "List available tools");
        response.put("sampleTools", tools);

        return ResponseEntity.ok(response);
    }
}
