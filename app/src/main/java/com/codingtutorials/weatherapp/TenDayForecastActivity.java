package com.codingtutorials.weatherapp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TenDayForecastActivity extends AppCompatActivity {

    private LinearLayout forecastContainer;
    private static final String API_KEY = "effebe0badd5d381a1fc85ffb3b2740d";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ten_day_forecast);

        forecastContainer = findViewById(R.id.forecastContainer);
        findViewById(R.id.backToMainButton).setOnClickListener(v -> finish());

        String cityName = getIntent().getStringExtra("city_name");
        if (cityName == null || cityName.isEmpty()) {
            cityName = "Bishkek";
        }

        fetchCoordinatesAndShowForecast(cityName);
    }

    private void fetchCoordinatesAndShowForecast(String cityName) {
        String geoUrl = "https://api.openweathermap.org/geo/1.0/direct?q=" + cityName + "&limit=1&appid=" + API_KEY;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(geoUrl).build();
                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                JSONArray geoArray = new JSONArray(responseBody);

                if (geoArray.length() > 0) {
                    JSONObject location = geoArray.getJSONObject(0);
                    double lat = location.getDouble("lat");
                    double lon = location.getDouble("lon");
                    fetch10DayForecast(lat, lon);
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> showError("Не удалось получить данные о городе"));
            }
        });
    }

    private void fetch10DayForecast(double lat, double lon) {
        String forecastUrl = "https://api.openweathermap.org/data/3.0/onecall?lat=" + lat +
                "&lon=" + lon + "&exclude=current,minutely,hourly,alerts&units=metric&lang=ru&appid=" + API_KEY;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(forecastUrl).build();
                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                JSONObject forecastJson = new JSONObject(responseBody);
                JSONArray dailyArray = forecastJson.getJSONArray("daily");
                runOnUiThread(() -> showForecastOnUI(dailyArray));
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> showError("Не удалось получить прогноз погоды"));
            }
        });
    }

    private void showForecastOnUI(JSONArray dailyArray) {
        forecastContainer.removeAllViews();

        try {
            for (int i = 0; i < Math.min(10, dailyArray.length()); i++) {
                JSONObject day = dailyArray.getJSONObject(i);

                long dt = day.getLong("dt");
                JSONObject temp = day.getJSONObject("temp");
                double min = temp.getDouble("min");
                double max = temp.getDouble("max");
                String description = day.getJSONArray("weather").getJSONObject(0).getString("description");
                String iconCode = day.getJSONArray("weather").getJSONObject(0).getString("icon");

                View forecastItem = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastContainer, false);

                // Format date
                Date date = new Date(dt * 1000L);
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", new Locale("ru"));
                String formattedDate = sdf.format(date);

                // Capitalize first letter of description
                description = description.substring(0, 1).toUpperCase() + description.substring(1);

                // Set data to views
                TextView dateText = forecastItem.findViewById(R.id.dateText);
                TextView descriptionText = forecastItem.findViewById(R.id.descriptionText);
                TextView tempRangeText = forecastItem.findViewById(R.id.tempRangeText);
                ImageView weatherIcon = forecastItem.findViewById(R.id.weatherIcon);

                dateText.setText(formattedDate);
                descriptionText.setText(description);
                tempRangeText.setText(String.format(Locale.getDefault(), "%.0f° / %.0f°", min, max));

                // Set weather icon
                int iconResId = getResources().getIdentifier("ic_" + iconCode, "drawable", getPackageName());
                weatherIcon.setImageResource(iconResId);

                forecastContainer.addView(forecastItem);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            showError("Ошибка обработки данных");
        }
    }

    private void showError(String message) {
        TextView errorView = new TextView(this);
        errorView.setText(message);
        errorView.setTextColor(Color.WHITE);
        errorView.setTextSize(16);
        errorView.setGravity(Gravity.CENTER);
        forecastContainer.addView(errorView);
    }
}