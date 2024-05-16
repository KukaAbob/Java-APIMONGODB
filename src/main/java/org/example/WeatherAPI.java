package org.example;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherAPI {
    public static void main(String[] args) {
        String connectionString = "mongodb+srv://kukakarakuzov:Kuanis2006@cluster0.wjekppx.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        // Create a new client and connect to the server
        try (MongoClient mongoClient = MongoClients.create(settings)) {
            try {
                // Send a ping to confirm a successful connection
                MongoDatabase database = mongoClient.getDatabase("admin");
                database.runCommand(new Document("ping", 1));
                System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
            } catch (MongoException e) {
                e.printStackTrace();
            }
        }
        try {
            // Замените YOUR_API_KEY на ваш API ключ
            String apiKey = "301ce6a4f94c2e9d5a0aa5b33f548a8b";
            // Замените YOUR_CITY на ваш город
            String city = "Astana";

            // URL для запроса погодных данных
            String urlString = "https://api.openweathermap.org/data/2.5/forecast?q=" + city + "&appid=" + apiKey;

            // Создание объекта URL
            URL url = new URL(urlString);

            // Открытие соединения
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Установка метода запроса
            connection.setRequestMethod("GET");

            // Получение ответа от сервера
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Преобразование ответа в объект JSON
            JSONObject jsonResponse = new JSONObject(response.toString());

            // Получение списка прогнозов погоды
            JSONArray forecasts = jsonResponse.getJSONArray("list");

            // Получение данных о погоде для каждого дня недели
            for (int i = 0; i < 7; i++) {
                JSONObject forecast = forecasts.getJSONObject(i * 8); // Каждый прогноз на 3 часа, поэтому используем индекс i * 8
                // Получение даты и времени прогноза
                String dateTime = forecast.getString("dt_txt");

                // Получение данных о погоде
                JSONObject main = forecast.getJSONObject("main");
                double temperature = main.getDouble("temp") - 273.15; // Преобразование в градусы Цельсия
                double humidity = main.getDouble("humidity");

                JSONObject wind = forecast.getJSONObject("wind");
                double windSpeed = wind.getDouble("speed");

                // Запись данных в базу данных
                try (MongoClient mongoClient = MongoClients.create(settings)) {
                    MongoDatabase database = mongoClient.getDatabase("weatherDB");
                    database.getCollection("forecasts").insertOne(new Document()
                            .append("DateTime", dateTime)
                            .append("Temperature", temperature)
                            .append("Humidity", humidity)
                            .append("WindSpeed", windSpeed));
                } catch (MongoException e) {
                    e.printStackTrace();
                }

                // Вывод данных
                System.out.println("DateTime: " + dateTime);
                System.out.println("Temperature: " + temperature + " °C");
                System.out.println("Humidity: " + humidity + " %");
                System.out.println("Wind Speed: " + windSpeed + " m/s");
                System.out.println("Data for day " + (i + 1) + " has been successfully saved to the database.");
                System.out.println("---------------------");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
