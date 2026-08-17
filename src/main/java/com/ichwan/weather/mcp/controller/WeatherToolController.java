package com.ichwan.weather.mcp.controller;

import com.ichwan.weather.mcp.model.Coordinates;
import com.ichwan.weather.mcp.model.CurrentWeather;
import com.ichwan.weather.mcp.service.GeocodingService;
import com.ichwan.weather.mcp.service.WeatherService;
import com.ichwan.weather.mcp.view.WeatherView;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

public class WeatherToolController {

    private final GeocodingService geocodingService = new GeocodingService();
    private final WeatherService weatherService = new WeatherService();
    private final WeatherView view = new WeatherView();

    public McpSchema.CallToolResult getCurrentWeather(McpSchema.CallToolRequest request) {
        String city = (String) request.arguments().get("city");

        try {
            Coordinates coords = geocodingService.geocode(city);
            if (coords == null) {
                return errorResult("Kota '" + city + "' tidak ditemukan. Coba nama kota lain.");
            }

            CurrentWeather weather = weatherService.fetchCurrentWeather(coords, city);
            String text = view.render(weather);

            return McpSchema.CallToolResult.builder()
                    .content(List.of(new McpSchema.TextContent(text)))
                    .build();

        } catch (Exception e) {
            return errorResult("Gagal mengambil data cuaca: " + e.getMessage());
        }
    }

    private McpSchema.CallToolResult errorResult(String message) {
        return McpSchema.CallToolResult.builder()
                .isError(true)
                .content(List.of(new McpSchema.TextContent(message)))
                .build();
    }

}
