package ru.ryabov.pet.application.retrofit.services.weather.dto;

import java.util.List;

import lombok.Data;

@Data
public class WeatherResponse {
    private Coordinate coord;
    private List<Weather> weather;
    private String base;
    private Main main;
    private int visibility;
    private Wind wind;
    private Clouds clouds;
    private int dt;
    private Sys sys;
    private int timezone;
    private int id;
    private String name;
    private int cod;

    @Data
    public class Coordinate {
        private double lon;
        private double lat;
    }

    @Data
    public class Weather {
        private int id;
        private String main;
        private String description;
        private String icon;
    }

    @Data
    public class Main {
        private double temp;
        private double feels_like;
        private double temp_min;
        private double temp_max;
        private int pressure;
        private int humidity;
    }

    @Data
    public class Wind {
        private double speed;
        private int deg;
        private double gust;
    }

    @Data
    public class Clouds {
        private int all;
    }

    @Data
    public class Sys {
        private int type;
        private int id;
        private String country;
        private int sunrise;
        private int sunset;
    }
}
