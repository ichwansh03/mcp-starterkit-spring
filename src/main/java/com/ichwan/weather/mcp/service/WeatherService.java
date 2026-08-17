package com.ichwan.weather.mcp.service;

import com.ichwan.weather.mcp.model.Coordinates;
import com.ichwan.weather.mcp.model.CurrentWeather;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public CurrentWeather fetchCurrentWeather(Coordinates coords, String cityLabel) throws Exception {
        String url = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true",
                coords.latitude(), coords.longitude());

        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

        JSONObject current = new JSONObject(resp.body()).getJSONObject("current_weather");

        return new CurrentWeather(
                cityLabel,
                current.getDouble("temperature"),
                current.getDouble("windspeed"),
                current.getInt("weathercode")
        );
    }

}
