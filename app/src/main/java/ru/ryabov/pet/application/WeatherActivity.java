package ru.ryabov.pet.application;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import lombok.SneakyThrows;
import ru.ryabov.pet.application.databinding.ActivityWeatherBinding;
import ru.ryabov.pet.application.retrofit.services.weather.WeatherManager;
import ru.ryabov.pet.application.retrofit.services.weather.dto.WeatherResponse;

public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = "WeatherActivity";
    private WeatherManager weatherManager;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mCityView;
    private View mTemperatureView;
    private View mSomeView;
    private View mDescriptionView;
    private ActivityWeatherBinding binding;
    private View mControlsView;
    private boolean mVisible;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mTemperatureView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };


    @SneakyThrows
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherManager = new WeatherManager();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        binding = ActivityWeatherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mVisible = true;
        mControlsView = binding.fullscreenContentControls;
        mCityView = binding.cityContent;
        mTemperatureView = binding.temperatureContent;
        mSomeView = binding.anotherContent;
        mDescriptionView = binding.descriptionContent;

        mTemperatureView.setOnClickListener(view -> toggle());
        mSomeView.setOnClickListener(view -> toggle());
        mDescriptionView.setOnClickListener(view -> toggle());

        checkPermissionsAndRequestIfNotExist();

        Executor executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(
                () -> {
                    WeatherResponse weatherResponse = getAsyncWeather();
                    while (weatherResponse == null) {
                        try {
                            Thread.currentThread().sleep(100);
                        } catch (InterruptedException e) {
                            Log.d(TAG, e.toString());
                        }
                    }
                    handler.post(() -> updateWeatherView(weatherResponse));
                }
        );

        binding.getWeather.setOnClickListener(v -> {
            WeatherResponse weatherResponse = getAsyncWeather();
            updateWeatherView(weatherResponse);
        });
    }

    private void updateWeatherView(WeatherResponse weatherResponse) {
        switch (weatherResponse.getWeather().get(0).getMain()) {
            case "Clouds":
                binding.getRoot().setBackground(getDrawable(R.drawable.sunny));
                break;
            case "Rain":
                binding.getRoot().setBackground(getDrawable(R.drawable.rain));
                break;
            case "Snow":
                binding.getRoot().setBackground(getDrawable(R.drawable.snow));
                break;
            case "Extreme":
                binding.getRoot().setBackground(getDrawable(R.drawable.windy));
                break;
            default:
                binding.getRoot().setBackground(getDrawable(R.drawable.weather));
        }
        if (weatherResponse.getWeather().get(0).getMain().equals("Clouds")) {
            binding.getRoot().setBackground(getDrawable(R.drawable.sunny));
        } else {
            binding.getRoot().setBackground(getDrawable(R.drawable.windy));
        }
        ((TextView) mCityView).setText(weatherResponse.getName());
        ((TextView) mTemperatureView).setText(String.format("%s°", weatherResponse.getMain().getTemp()));
        ((TextView) mSomeView).setText(String.format(
                "%s°/%s° Feels Like %s°",
                weatherResponse.getMain().getTemp_max(),
                weatherResponse.getMain().getTemp_min(),
                weatherResponse.getMain().getFeels_like()
        ));
        ((TextView) mDescriptionView).setText(String.format("%s", weatherResponse.getWeather().get(0).getDescription()));
    }


    @SneakyThrows
    private WeatherResponse getAsyncWeather() {
        checkPermissionsAndRequestIfNotExist();
        while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            try {
                Thread.currentThread().sleep(100);
            } catch (InterruptedException e) {
                Log.d(TAG, e.toString());
            }
        }
        Future<WeatherResponse> weatherByCoordinates = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            weatherByCoordinates = weatherManager.getWeatherByCoordinates(fusedLocationClient.getLastLocation());
        }
        if (weatherByCoordinates != null && weatherByCoordinates.get() != null) {
            return weatherByCoordinates.get();
        } else {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            AtomicReference<Location> locationGps = new AtomicReference<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, Executors.newSingleThreadExecutor(), locationGps::set);
            } else {
                locationGps = new AtomicReference<>(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
            }
            Location locationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            Location location;

            if (locationGps.get() != null) {
                location = locationGps.get();
            } else if (locationNetwork != null) {
                location = locationNetwork;
            } else {
                return null;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return weatherManager.getWeatherByCoordinates(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude())).get();
            } else {
                return weatherManager.getWeatherTaskByCoordinates(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude())).get();
            }
        }
    }

    private void checkPermissionsAndRequestIfNotExist() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.runOnUiThread(() -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0));
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        mTemperatureView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(this::hide);
        mHideHandler.postDelayed(this::hide, delayMillis);
    }
}