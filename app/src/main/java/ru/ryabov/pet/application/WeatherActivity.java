package ru.ryabov.pet.application;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import lombok.SneakyThrows;
import ru.ryabov.pet.application.databinding.ActivityWeatherBinding;
import ru.ryabov.pet.application.retrofit.services.weather.WeatherManager;
import ru.ryabov.pet.application.retrofit.services.weather.dto.WeatherResponse;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@SuppressLint("MissingPermission")
public class WeatherActivity extends AppCompatActivity {

    private WeatherManager weatherManager;
    private FusedLocationProviderClient fusedLocationClient;
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mTemperatureView;
    private View mSomeView;
    private View mDescriptionView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mTemperatureView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };


    private ActivityWeatherBinding binding;

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
        mTemperatureView = binding.temperatureContent;
        mSomeView = binding.anotherContent;
        mDescriptionView = binding.descriptionContent;

        mTemperatureView.setOnClickListener(view -> toggle());
        mSomeView.setOnClickListener(view -> toggle());
        mDescriptionView.setOnClickListener(view -> toggle());

        while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

        Executor executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(
                () -> {
                    WeatherResponse weatherResponse = getAsyncWeather();
                    handler.post(() -> updateWeatherView(Objects.requireNonNull(weatherResponse)));
                }
        );

        binding.getWeather.setOnClickListener(v -> {
            WeatherResponse weatherResponse = getAsyncWeather();
            updateWeatherView(Objects.requireNonNull(weatherResponse));
        });
    }

    private void updateWeatherView(WeatherResponse weatherResponse) {
        if (weatherResponse.getWeather().get(0).getMain().equals("Clouds")) {
            binding.getRoot().setBackground(getDrawable(R.drawable.sunny));
        } else {
            binding.getRoot().setBackground(getDrawable(R.drawable.windy));
        }
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
        Future<WeatherResponse> weatherByCoordinates = weatherManager.getWeatherByCoordinates(fusedLocationClient.getLastLocation());
        if (weatherByCoordinates.get() != null) {
            return weatherByCoordinates.get();
        } else {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            AtomicReference<Location> locationGps = new AtomicReference<>();
            locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, Executors.newSingleThreadExecutor(), locationGps::set);
//                Location locationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            Location location;

            if (locationGps.get() != null) {
                location = locationGps.get();
            } else if (locationNetwork != null) {
                location = locationNetwork;
            } else {
                return null;
            }
            return weatherManager.getWeatherByCoordinates(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude())).get();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
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
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        mTemperatureView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}