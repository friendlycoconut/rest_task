package com.kost.rest_task.controllers;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
class AirportController {

    @Value("${url.template.airport}")
    private String airportUri;

    @Value("${url.template.weather}")
    private String weatherUri;


    private UriTemplate weatherUriTemplate;


    @GetMapping("/queryAirportTemp")
    public ResponseEntity<Map<String, Object>> getAirportData(@RequestParam String iata) throws IOException, InterruptedException {

        weatherUriTemplate = new UriTemplate(weatherUri);
        Map<String, String> templateVariables = new HashMap<>();
        URL urlAirport = new URL(airportUri + iata);

        String json = IOUtils.toString(urlAirport, Charset.forName("UTF-8"));
        JSONObject retrievedObject = new JSONObject(json);

        templateVariables.put("latitude", retrievedObject.getString("latitude")); //could use a variable here
        templateVariables.put("longitude", retrievedObject.getString("longitude")); //could use a variable here

        URI weatherServiceUri = weatherUriTemplate.expand(templateVariables);

        String jsonWeather = IOUtils.toString(weatherServiceUri, Charset.forName("UTF-8"));
        JSONObject weatherRetrievedObject = new JSONObject(jsonWeather);
        JSONObject currentWeatherObject = (JSONObject) weatherRetrievedObject.get("current_weather");

        System.out.println(currentWeatherObject);
        JSONObject jsonResult = new JSONObject();
        jsonResult.put("result", currentWeatherObject.get("temperature"));

        return new ResponseEntity<>(
                jsonResult.toMap(),
                HttpStatus.OK
        );
    }

    @GetMapping("/queryStockPrice")
    public ResponseEntity<Map<String, Object>> getStockMarketData(@RequestParam String stock) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mboum-finance.p.rapidapi.com/qu/quote?symbol=" + stock))
                .header("content-type", "application/octet-stream")
                .header("X-RapidAPI-Key", "")
                .header("X-RapidAPI-Host", "mboum-finance.p.rapidapi.com")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        JSONArray array = new JSONArray(response.body());
        JSONObject jsonObject = new JSONObject();
        for (int i = 0; i < array.length(); i++) {
            jsonObject = array.getJSONObject(i);
        }


        JSONObject jsonResult = new JSONObject();
        jsonResult.put("result", jsonObject.get("regularMarketPrice"));


        return new ResponseEntity<>(
                jsonResult.toMap(),
                HttpStatus.OK
        );
    }


    @GetMapping("/queryEval")
    public ResponseEntity<Map<String, Object>> getQueryEval(@RequestParam String query) throws IOException, InterruptedException, ScriptException {
        String queryString = query.replaceAll(" ", "+").toLowerCase();
        System.out.println(queryString);
        Expression expression = new ExpressionBuilder(queryString).build();
        double result = expression.evaluate();
        JSONObject jsonResult = new JSONObject();
        jsonResult.put("result", result);

        return new ResponseEntity<>(
                jsonResult.toMap(),
                HttpStatus.OK
        );
    }

}
