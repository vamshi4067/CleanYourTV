package com.gathin.tvhome;

import android.content.SharedPreferences;
import android.os.SystemClock;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class CricketWeather {
    private static final long PERIOD = 30 * 60_000L;
    private final String forecast;
    private final TextView label;
    private final SharedPreferences prefs;
    private final Runnable tick = this::refresh;
    private boolean active;

    CricketWeather(TextView label, SharedPreferences prefs) {
        this.label = label;
        this.prefs = prefs;
        String latitude = label.getContext().getString(R.string.weather_latitude).trim();
        String longitude = label.getContext().getString(R.string.weather_longitude).trim();
        String endpoint = null;
        try {
            double lat = Double.parseDouble(latitude);
            double lon = Double.parseDouble(longitude);
            if (Double.isFinite(lat) && Double.isFinite(lon) && Math.abs(lat) <= 90 && Math.abs(lon) <= 180) {
                endpoint = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                        + "&current=temperature_2m,weather_code&temperature_unit=celsius";
            }
        } catch (NumberFormatException ignored) {
            // Location is deliberately unset in the shareable build.
        }
        forecast = endpoint;
        label.setText(prefs.getString("weather_label", ""));
    }

    void start() {
        if (forecast == null) { label.setText(""); return; }
        active = true;
        refresh();
    }

    void stop() {
        active = false;
        label.removeCallbacks(tick);
    }

    private void refresh() {
        if (!active) return;
        label.removeCallbacks(tick);
        long now = SystemClock.elapsedRealtime();
        long last = prefs.getLong("weather_attempt", -PERIOD);
        long delay = last <= now ? PERIOD - (now - last) : 0;
        if (delay > 0) {
            label.postDelayed(tick, delay);
            return;
        }
        prefs.edit().putLong("weather_attempt", now).apply();
        label.postDelayed(tick, PERIOD);
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(forecast).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                try (InputStream input = connection.getInputStream(); ByteArrayOutputStream body = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        body.write(buffer, 0, read);
                        if (body.size() > 16384) return;
                    }
                    JSONObject current = new JSONObject(new String(body.toByteArray(), StandardCharsets.UTF_8))
                            .getJSONObject("current");
                    int code = current.getInt("weather_code");
                    String icon = code == 0 ? "☼" : code <= 3 || code == 45 || code == 48 ? "☁" : "☂";
                    String display = icon + "  " + Math.round(current.getDouble("temperature_2m")) + "°C";
                    prefs.edit().putString("weather_label", display).apply();
                    label.post(() -> label.setText(display));
                }
            } catch (Exception ignored) {
                // Keep the last successful reading during a network outage.
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "cricket-weather").start();
    }
}
