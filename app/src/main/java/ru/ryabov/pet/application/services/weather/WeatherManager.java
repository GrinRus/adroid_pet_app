package ru.ryabov.pet.application.services.weather;

import lombok.SneakyThrows;
import ru.ryabov.pet.application.services.ServiceManager;
import ru.ryabov.pet.application.services.weather.dto.Weather;

public class WeatherManager extends ServiceManager {

    private final String appid = "a";
    private final WeatherService service;

    public WeatherManager() {
        this.service = createService(WeatherService.class, "api.openweathermap.org");
    }

    @SneakyThrows
    public Weather getWeatherByCoordinates(String lat, String lon){
       return service.getWeatherByCoordinates(lat, lon, appid).execute().body();
    }
}
