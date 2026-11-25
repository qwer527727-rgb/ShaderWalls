package com.shaderwalls.app;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

/** Fetches current weather + sunrise/sunset from Open-Meteo (free, no API key). */
public final class Weather {
    private Weather() {}

    private static final long STALE_MS = 30L * 60L * 1000L;
    private static volatile boolean busy;

    public static void refreshIfStale(Context c) {
        long last = Styles.prefs(c).getLong("w_time", 0);
        if (System.currentTimeMillis() - last > STALE_MS) refreshAsync(c);
    }

    public static void refreshAsync(final Context ctx) {
        if (busy) return;
        busy = true;
        final Context c = ctx.getApplicationContext();
        new Thread(() -> {
            try { fetch(c); } catch (Exception ignored) { } finally { busy = false; }
        }).start();
    }

    public static boolean hasLocation(Context c) {
        return Styles.prefs(c).contains("loc_lat");
    }

    public static void saveLocation(Context c, double lat, double lon, String name) {
        Styles.prefs(c).edit()
                .putFloat("loc_lat", (float) lat)
                .putFloat("loc_lon", (float) lon)
                .putString("loc_name", name)
                .putLong("w_time", 0)
                .apply();
    }

    /** Best location: newest last-known fix if we have permission, else the saved one. */
    private static double[] location(Context c) {
        SharedPreferences p = Styles.prefs(c);
        if (!p.getBoolean("loc_city", false)
                && c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
                Location best = null;
                for (String prov : lm.getProviders(true)) {
                    Location l = lm.getLastKnownLocation(prov);
                    if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
                }
                if (best != null) {
                    p.edit().putFloat("loc_lat", (float) best.getLatitude())
                            .putFloat("loc_lon", (float) best.getLongitude()).apply();
                    return new double[]{best.getLatitude(), best.getLongitude()};
                }
            } catch (SecurityException ignored) { }
        }
        if (p.contains("loc_lat")) return new double[]{p.getFloat("loc_lat", 0), p.getFloat("loc_lon", 0)};
        return null;
    }

    public static boolean fetch(Context c) throws Exception {
        double[] ll = location(c);
        if (ll == null) return false;
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                        + "&current=temperature_2m,weather_code,cloud_cover,is_day"
                        + "&daily=sunrise,sunset&timezone=auto&forecast_days=1", ll[0], ll[1]);
        JSONObject j = new JSONObject(get(url));
        JSONObject cur = j.getJSONObject("current");
        JSONObject daily = j.getJSONObject("daily");
        Styles.prefs(c).edit()
                .putInt("w_code", cur.optInt("weather_code", 0))
                .putInt("w_cloud", cur.optInt("cloud_cover", -1))
                .putFloat("w_temp", (float) cur.optDouble("temperature_2m", 0))
                .putInt("w_off", j.optInt("utc_offset_seconds", 0))
                .putInt("w_sr", hm(daily.getJSONArray("sunrise").getString(0), 360))
                .putInt("w_ss", hm(daily.getJSONArray("sunset").getString(0), 1080))
                .putLong("w_time", System.currentTimeMillis())
                .apply();
        return true;
    }

    /** City search. Returns {name, lat, lon} or null. */
    public static String[] geocode(String name) throws Exception {
        String url = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=ar&name="
                + URLEncoder.encode(name.trim(), "UTF-8");
        JSONObject j = new JSONObject(get(url));
        JSONArray r = j.optJSONArray("results");
        if (r == null || r.length() == 0) return null;
        JSONObject o = r.getJSONObject(0);
        String n = o.optString("name", name);
        String country = o.optString("country", "");
        if (!country.isEmpty()) n = n + "، " + country;
        return new String[]{n, String.valueOf(o.getDouble("latitude")), String.valueOf(o.getDouble("longitude"))};
    }

    /** "2026-11-25T06:31" -> minutes of day. */
    private static int hm(String iso, int def) {
        try {
            int t = iso.indexOf('T');
            return Integer.parseInt(iso.substring(t + 1, t + 3)) * 60 + Integer.parseInt(iso.substring(t + 4, t + 6));
        } catch (Exception e) {
            return def;
        }
    }

    private static String get(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(10000);
        con.setReadTimeout(10000);
        try (InputStream in = con.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } finally {
            con.disconnect();
        }
    }

    public static String icon(int code) {
        if (code == 0) return "☀️";
        if (code <= 2) return "🌤";
        if (code == 3) return "☁️";
        if (code == 45 || code == 48) return "🌫";
        if (code >= 95) return "⛈";
        if (code >= 71 && code <= 77 || code == 85 || code == 86) return "❄️";
        return "🌧";
    }

    public static String summary(Context c) {
        SharedPreferences p = Styles.prefs(c);
        if (!p.contains("w_code")) {
            return hasLocation(c) ? "جارٍ جلب الطقس…" : "حدّد موقعك لتتفاعل السماء مع الطقس";
        }
        String place = p.getString("loc_name", "");
        return Weather.icon(p.getInt("w_code", 0)) + "  " + Math.round(p.getFloat("w_temp", 0)) + "°"
                + (place.isEmpty() ? "" : "  •  " + place);
    }
}
