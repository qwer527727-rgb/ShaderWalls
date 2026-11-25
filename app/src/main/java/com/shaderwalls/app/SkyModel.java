package com.shaderwalls.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

/** Turns time of day + weather into the Live Sky colours and effect strengths. */
public final class SkyModel {
    public final float[] sky = new float[9]; // top, mid, bottom (rgb)
    public float cloud, rain, snow, fog, storm, glow;

    // Palettes (top, middle, bottom) — inspired by "Collecting Sunsets"
    private static final int[] NIGHT   = {0x070F4A, 0x0A2A8A, 0x0B3DA8};
    private static final int[] DAWN_A  = {0x3A3F8F, 0x9C8AC0, 0xF2A99A};
    private static final int[] DAWN_B  = {0x8DA8DE, 0xEBC3B4, 0xFFC38A};
    private static final int[] DAY     = {0x4F8FE0, 0x8DB6EA, 0xD3E3F5};
    private static final int[] LATE    = {0x92B4E8, 0xF4C99A, 0xFF8F4E};
    private static final int[] SUNSET  = {0xD7BE9F, 0xFDB66A, 0xFF6A4B};
    private static final int[] DUSK    = {0x232A78, 0x6E4E9C, 0xE07A62};

    // Manual overrides
    public static final String[] WEATHER_NAMES = {"تلقائي", "صافٍ", "غائم", "مطر", "ثلج", "عاصفة", "ضباب"};
    public static final String[] TIME_NAMES = {"تلقائي", "شروق", "نهار", "غروب", "ليل"};

    public static void compute(Context c, SkyModel out) {
        SharedPreferences p = Styles.prefs(c);
        int sr = p.getInt("w_sr", 6 * 60);
        int ss = p.getInt("w_ss", 18 * 60);
        if (ss <= sr) { sr = 360; ss = 1080; }

        int minute;
        if (p.contains("w_off")) {
            long sec = System.currentTimeMillis() / 1000L + p.getInt("w_off", 0);
            minute = (int) (((sec % 86400L) + 86400L) % 86400L / 60L);
        } else {
            Calendar cal = Calendar.getInstance();
            minute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        }
        switch (p.getInt("sky_time", 0)) {
            case 1: minute = sr + 10; break;
            case 2: minute = (sr + ss) / 2; break;
            case 3: minute = ss - 5; break;
            case 4: minute = (ss + 240) % 1440; break;
            default: break;
        }

        int[] keys = {sr - 90, sr - 25, sr + 25, sr + 120, ss - 120, ss - 45, ss, ss + 35, ss + 95};
        int[][] pals = {NIGHT, DAWN_A, DAWN_B, DAY, DAY, LATE, SUNSET, DUSK, NIGHT};
        float[] rgb = interpolate(keys, pals, minute);

        float g1 = gauss(minute - (ss - 10), 55f), g2 = gauss(minute - (sr + 10), 45f);
        float glow = Math.max(g1, g2 * 0.8f);

        // weather
        int mode = p.getInt("sky_weather", 0);
        float cl = 0, ra = 0, sn = 0, fo = 0, st = 0;
        if (mode == 0) {
            int code = p.getInt("w_code", 0);
            int cover = p.getInt("w_cloud", -1);
            if (code == 1) cl = 0.3f;
            else if (code == 2) cl = 0.55f;
            else if (code == 3) cl = 0.9f;
            else if (code == 45 || code == 48) { fo = 0.75f; cl = 0.4f; }
            else if (code >= 51 && code <= 57) { ra = 0.35f; cl = 0.8f; }
            else if (code >= 61 && code <= 67) { ra = code >= 65 ? 1f : 0.7f; cl = 0.9f; }
            else if (code >= 71 && code <= 77) { sn = code >= 75 ? 1f : 0.65f; cl = 0.75f; }
            else if (code >= 80 && code <= 82) { ra = 0.8f; cl = 0.85f; }
            else if (code == 85 || code == 86) { sn = 0.8f; cl = 0.8f; }
            else if (code >= 95) { ra = 0.95f; cl = 1f; st = 1f; }
            if (cover >= 0 && ra == 0 && sn == 0 && fo == 0) cl = Math.max(cl, cover / 100f * 0.9f);
        } else if (mode == 2) cl = 0.85f;
        else if (mode == 3) { cl = 0.9f; ra = 0.8f; }
        else if (mode == 4) { cl = 0.75f; sn = 0.85f; }
        else if (mode == 5) { cl = 1f; ra = 1f; st = 1f; }
        else if (mode == 6) { fo = 0.8f; cl = 0.35f; }

        // tint the sky for the weather
        for (int i = 0; i < 3; i++) {
            float r = rgb[i * 3], g = rgb[i * 3 + 1], b = rgb[i * 3 + 2];
            float l = 0.299f * r + 0.587f * g + 0.114f * b;
            float k = cl * 0.5f;
            r = r + (l - r) * k; g = g + (l - g) * k; b = b + (l - b) * k;
            float dark = 1f - 0.22f * ra - 0.25f * st;
            r *= dark; g *= dark; b *= dark;
            if (ra > 0) { r += (0.33f - r) * ra * 0.25f; g += (0.38f - g) * ra * 0.25f; b += (0.46f - b) * ra * 0.25f; }
            if (sn > 0) { r += (0.82f - r) * sn * 0.3f; g += (0.86f - g) * sn * 0.3f; b += (0.92f - b) * sn * 0.3f; }
            out.sky[i * 3] = r; out.sky[i * 3 + 1] = g; out.sky[i * 3 + 2] = b;
        }
        out.cloud = cl; out.rain = ra; out.snow = sn; out.fog = fo; out.storm = st;
        out.glow = glow * (1f - 0.8f * Math.min(1f, cl + ra + fo));
    }

    private static float gauss(float x, float w) {
        return (float) Math.exp(-(x * x) / (w * w));
    }

    private static float[] rgb(int c) {
        return new float[]{((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f};
    }

    private static float[] interpolate(int[] keys, int[][] pals, int m) {
        int n = keys.length;
        // keys span roughly one day; find the segment containing m (with wrap-around)
        for (int i = 0; i < n; i++) {
            int a = keys[i];
            int b = (i + 1 < n) ? keys[i + 1] : keys[0] + 1440;
            for (int shift = -1440; shift <= 1440; shift += 1440) {
                int mm = m + shift;
                if (mm >= a && mm < b) {
                    float t = (b == a) ? 0f : (mm - a) / (float) (b - a);
                    t = t * t * (3 - 2 * t);
                    int[] pa = pals[i], pb = pals[(i + 1) % n];
                    float[] out = new float[9];
                    for (int k = 0; k < 3; k++) {
                        float[] ca = rgb(pa[k]), cb = rgb(pb[k]);
                        for (int d = 0; d < 3; d++) out[k * 3 + d] = ca[d] + (cb[d] - ca[d]) * t;
                    }
                    return out;
                }
            }
        }
        float[] out = new float[9];
        for (int k = 0; k < 3; k++) System.arraycopy(rgb(NIGHT[k]), 0, out, k * 3, 3);
        return out;
    }
}
