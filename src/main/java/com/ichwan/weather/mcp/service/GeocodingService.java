package com.ichwan.weather.mcp.service;

import com.ichwan.weather.mcp.model.Coordinates;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GeocodingService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /** Mengembalikan null kalau kota tidak ditemukan. */
    public Coordinates geocode(String city) throws Exception {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1";

        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

        JSONObject json = new JSONObject(resp.body());
        if (!json.has("results")) {
            return null;
        }
        JSONArray results = json.getJSONArray("results");
        JSONObject first = results.getJSONObject(0);
        return new Coordinates(first.getDouble("latitude"), first.getDouble("longitude"));
    }

}
