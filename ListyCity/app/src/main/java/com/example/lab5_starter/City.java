package com.example.lab5_starter;
import androidx.annotation.NonNull;

public class City {
    private final String cityName;
    private final String provinceName;

    public City(String cityName, String provinceName) {
        this.cityName = cityName;
        this.provinceName = provinceName;
    }

    public String getCityName() {
        return cityName;
    }

    public String getProvinceName() {
        return provinceName;
    }

    @NonNull
    @Override
    public String toString() {
        return cityName + ", " + provinceName;
    }
}
