package com.ichwan.weather.mcp.view;

import com.ichwan.weather.mcp.model.CurrentWeather;

public class WeatherView {

    public String render(CurrentWeather weather) {
        return String.format(
                "Cuaca di %s: %.1f°C, angin %.1f km/jam, kondisi: %s",
                weather.city(),
                weather.temperature(),
                weather.windSpeed(),
                describeWeatherCode(weather.weatherCd())
        );
    }

    // Open-Meteo pakai kode angka standar WMO untuk kondisi cuaca.
    private String describeWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Cerah";
            case 1, 2, 3 -> "Berawan sebagian";
            case 45, 48 -> "Berkabut";
            case 51, 53, 55 -> "Gerimis";
            case 61, 63, 65 -> "Hujan";
            case 80, 81, 82 -> "Hujan deras";
            case 95, 96, 99 -> "Badai petir";
            default -> "Tidak diketahui (kode " + code + ")";
        };
    }

}
