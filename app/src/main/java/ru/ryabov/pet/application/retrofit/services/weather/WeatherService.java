package ru.ryabov.pet.application.retrofit.services.weather;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.ryabov.pet.application.retrofit.services.weather.dto.WeatherResponse;

public interface WeatherService {
    @GET("/data/2.5/weather")
    Call<WeatherResponse> getWeatherByCoordinates(@Query("lat") String lat, @Query("lon") String lon,@Query("units") String units, @Query("appid") String appid);
}
