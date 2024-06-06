package com.quran.quranaudio.online.prayertimes.openweathermap;

import java.util.List;


public class OpenWeatherMapResponse {

    private MainWeatherData main;
    private List<Weather> weather;

    public MainWeatherData getMain() {
        return main;
    }

    public void setMain(MainWeatherData main) {
        this.main = main;
    }

    public List<Weather> getWeather() {
        return weather;
    }

    public void setWeather(List<Weather> weather) {
        this.weather = weather;
    }
}
