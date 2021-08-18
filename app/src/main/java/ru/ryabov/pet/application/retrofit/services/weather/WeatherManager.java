package ru.ryabov.pet.application.retrofit.services.weather;

import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.Task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import lombok.SneakyThrows;
import ru.ryabov.pet.application.retrofit.services.ServiceManager;
import ru.ryabov.pet.application.retrofit.services.weather.dto.WeatherResponse;

public class WeatherManager extends ServiceManager {

    public static final String METRIC = "metric";
    public static final String OPEN_WEATHER_MAP_KEY = "15d04c757e4fe84f08ae5997360f77da";
    private final WeatherService service;

    public WeatherManager() {
        this.service = createService(WeatherService.class, "http://api.openweathermap.org");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public Future<WeatherResponse> getWeatherByCoordinates(String lat, String lon) {
        return CompletableFuture.supplyAsync(() -> getBody(lat, lon));
    }

    @SneakyThrows
    private WeatherResponse getBody(String lat, String lon) {
        return service.getWeatherByCoordinates(lat, lon, METRIC, WeatherManager.OPEN_WEATHER_MAP_KEY).execute().body();
    }

    @SneakyThrows
    private WeatherResponse getBody(Task<Location> lastLocation) {
        while (!lastLocation.isComplete()){
            Log.d("some", "getBody: d");
        }
        Location location = lastLocation.getResult();
        if (location == null) {
            return null;
        }
        String lat = String.valueOf(location.getLatitude());
        String lon = String.valueOf(location.getLongitude());
        return service.getWeatherByCoordinates(lat, lon, METRIC, WeatherManager.OPEN_WEATHER_MAP_KEY).execute().body();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public Future<WeatherResponse> getWeatherByCoordinates(Task<Location> lastLocation) {
        return CompletableFuture.supplyAsync(() -> getBody(lastLocation));
    }
}
