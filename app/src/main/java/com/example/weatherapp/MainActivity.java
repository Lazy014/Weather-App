package com.example.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private RelativeLayout home;
    private ProgressBar progressBar;
    private TextView cityText;
    private TextView temperatureText;
    private TextView conditionText;
    private TextInputEditText cityInput;
    private ImageView background, viIcon, searchIcon;
    private RecyclerView weatherForecasts;

    private ArrayList<WeatherModel> weatherModelArrayList;
    private WeatherAdapter weatherAdapter;
    private LocationManager locationManager;

    private int PERMISSION_CODE = 1;
    private String cityName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);
        home = findViewById(R.id.home);
        progressBar = findViewById(R.id.loading);
        cityText = findViewById(R.id.city_name);
        temperatureText = findViewById(R.id.temperature_text);
        conditionText = findViewById(R.id.condition);
        cityInput = findViewById(R.id.city);
        background = findViewById(R.id.background);
        viIcon = findViewById(R.id.vi_icon);
        searchIcon = findViewById(R.id.search_icon);
        weatherForecasts = findViewById(R.id.weathers_view);

        weatherModelArrayList = new ArrayList<>();
        weatherAdapter = new WeatherAdapter(this, weatherModelArrayList);
        weatherForecasts.setAdapter(weatherAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_CODE);
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        cityName = getCityName(location.getLongitude(), location.getLatitude());
        getWeatherInfo(cityName);
        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String city = cityInput.getText().toString();
                if (city.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter city name", Toast.LENGTH_SHORT).show();
                } else {
                    cityText.setText(cityName);
                    getWeatherInfo(city);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Provide permissions please.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String getCityName(double longitude, double latitude) {
        String cityName = "Not Found";
        Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());

        try {
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 10);

            for (Address address : addressList) {
                if (address != null) {
                    String city = address.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                    } else {
                        Toast.makeText(this, "City not found!!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    private void getWeatherInfo(String cityName) {
        String url = "http://api.weatherapi.com/v1/forecast.json?key=5b6149b4522a42a296670227232105&q=" + cityName + "&days=1&aqi=no&alerts=no\n";
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                home.setVisibility(View.VISIBLE);
                weatherModelArrayList.clear();

                try {
                    String temp = response.getJSONObject("current").getString("temp_c");
                    temperatureText.setText(temp + "°C");
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    String condition_text = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String condition_icon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("http:".concat(condition_icon)).into(viIcon);
                    conditionText.setText(condition_text);
                    if (isDay == 1) {
                        Picasso.get().load("https://unsplash.com/photos/Uvaz8IGJ_7w").into(background);
                    } else {
                        Picasso.get().load("https://unsplash.com/photos/9sxeKzuCVoE").into(background);
                    }

                    JSONObject forecastObj = response.getJSONObject("forecast");
                    JSONObject forecast0 = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArr = forecast0.getJSONArray("hour");

                    for (int i = 0; i < hourArr.length(); i++) {
                        JSONObject hourObj = hourArr.getJSONObject(i);
                        String time = hourObj.getString("time");
                        String temper = hourObj.getString("temp_c");
                        String img = hourObj.getJSONObject("condition").getString("icon");
                        String wind = hourObj.getString("wind_kph");
                        weatherModelArrayList.add(new WeatherModel(time, temper, img, wind));
                    }
                    weatherAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Please enter valid city name.", Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }
}