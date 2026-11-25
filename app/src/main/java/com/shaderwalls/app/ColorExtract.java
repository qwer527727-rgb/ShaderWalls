package com.shaderwalls.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;

import java.io.InputStream;
import java.util.Arrays;

/** Extracts 5 dominant colours from a photo (k-means), sorted dark -> light. */
public final class ColorExtract {
    private ColorExtract() {}

    public static int[] fromUri(Context c, Uri uri) throws Exception {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, o);
        }
        int sample = 1;
        while (Math.max(o.outWidth, o.outHeight) / sample > 512) sample *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = sample;
        Bitmap b;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            b = BitmapFactory.decodeStream(in, null, o2);
        }
        if (b == null) throw new IllegalArgumentException("not an image");
        Bitmap small = Bitmap.createScaledBitmap(b, 64, 64, true);
        int[] px = new int[64 * 64];
        small.getPixels(px, 0, 64, 0, 0, 64, 64);
        if (small != b) small.recycle();
        b.recycle();
        return kmeans(px, 5);
    }

    private static float lum(float r, float g, float b) {
        return 0.299f * r + 0.587f * g + 0.114f * b;
    }

    static int[] kmeans(int[] px, int k) {
        int n = px.length;
        float[][] p = new float[n][3];
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            p[i][0] = Color.red(px[i]);
            p[i][1] = Color.green(px[i]);
            p[i][2] = Color.blue(px[i]);
            order[i] = i;
        }
        // init centres at luminance quantiles so the palette spans dark -> light
        Arrays.sort(order, (a, bb) -> Float.compare(lum(p[a][0], p[a][1], p[a][2]), lum(p[bb][0], p[bb][1], p[bb][2])));
        float[][] cen = new float[k][3];
        for (int j = 0; j < k; j++) cen[j] = p[order[(int) ((j + 0.5f) / k * (n - 1))]].clone();

        int[] asg = new int[n];
        for (int it = 0; it < 12; it++) {
            for (int i = 0; i < n; i++) {
                float best = Float.MAX_VALUE;
                for (int j = 0; j < k; j++) {
                    float dr = p[i][0] - cen[j][0], dg = p[i][1] - cen[j][1], db = p[i][2] - cen[j][2];
                    float d = dr * dr + dg * dg + db * db;
                    if (d < best) { best = d; asg[i] = j; }
                }
            }
            float[][] sum = new float[k][3];
            int[] cnt = new int[k];
            for (int i = 0; i < n; i++) {
                int j = asg[i];
                sum[j][0] += p[i][0]; sum[j][1] += p[i][1]; sum[j][2] += p[i][2];
                cnt[j]++;
            }
            for (int j = 0; j < k; j++) {
                if (cnt[j] > 0) for (int d = 0; d < 3; d++) cen[j][d] = sum[j][d] / cnt[j];
            }
        }
        Integer[] idx = new Integer[k];
        for (int j = 0; j < k; j++) idx[j] = j;
        final float[][] cc = cen;
        Arrays.sort(idx, (a, bb) -> Float.compare(lum(cc[a][0], cc[a][1], cc[a][2]), lum(cc[bb][0], cc[bb][1], cc[bb][2])));
        int[] out = new int[k];
        for (int j = 0; j < k; j++) {
            float[] v = cen[idx[j]];
            out[j] = Color.rgb(Math.round(v[0]), Math.round(v[1]), Math.round(v[2]));
        }
        return out;
    }
}
