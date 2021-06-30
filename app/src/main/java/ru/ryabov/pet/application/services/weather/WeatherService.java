package ru.ryabov.pet.application.services.weather;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.ryabov.pet.application.services.weather.dto.Weather;

public interface WeatherService {
    @GET("/data/2.5/weather")
    Call<Weather> getWeatherByCoordinates(@Query("lat") String lat, @Query("lon") String lon, @Query("appid") String appid);
}
