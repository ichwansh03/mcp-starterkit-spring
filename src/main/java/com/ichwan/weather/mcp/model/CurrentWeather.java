package com.ichwan.weather.mcp.model;

public record CurrentWeather(String city, double temperature, double windSpeed, int weatherCd) {
}
