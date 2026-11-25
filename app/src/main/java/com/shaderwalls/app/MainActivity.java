package com.shaderwalls.app;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private GLSurfaceView preview;
    private ShaderRenderer renderer;
    private TextView title;
    private final List<View> tiles = new ArrayList<>();
    private int selected;

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Window w = getWindow();
        w.setStatusBarColor(0xFF0E0E12);
        w.setNavigationBarColor(0xFF0E0E12);
        selected = Styles.current(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0E0E12);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));

        title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        // Phone-shaped (portrait) live preview
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        topLp.topMargin = dp(12);
        topLp.bottomMargin = dp(12);
        root.addView(top, topLp);

        final FrameLayout phone = new FrameLayout(this);
        phone.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(22));
            }
        });
        phone.setClipToOutline(true);
        top.addView(phone, new LinearLayout.LayoutParams(0, -1));
        top.addOnLayoutChangeListener((v, l, t, r, bo, ol, ot, or, ob) -> {
            int h = bo - t;
            int wpx = Math.min(r - l, Math.round(h * 9f / 19.5f));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) phone.getLayoutParams();
            if (lp.width != wpx) {
                lp.width = wpx;
                phone.post(() -> phone.setLayoutParams(lp));
            }
        });

        preview = new GLSurfaceView(this);
        preview.setEGLContextClientVersion(2);
        renderer = new ShaderRenderer(this, selected);
        preview.setRenderer(renderer);
        phone.addView(preview, new FrameLayout.LayoutParams(-1, -1));

        // Style picker grid
        ScrollView scroll = new ScrollView(this);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, dp(170)));

        LinearLayout row = null;
        for (int i = 0; i < Styles.FILES.length; i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
            }
            TextView t = new TextView(this);
            t.setText(Styles.NAMES[i]);
            t.setTextSize(12);
            t.setGravity(Gravity.CENTER);
            t.setTextColor(Styles.LIGHT[i] ? 0xFF222222 : Color.WHITE);
            final int idx = i;
            t.setOnClickListener(v -> select(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            row.addView(t, lp);
            tiles.add(t);
        }

        Button set = new Button(this);
        set.setText("تعيين كخلفية  •  Set wallpaper");
        set.setAllCaps(false);
        set.setTextColor(Color.BLACK);
        set.setTextSize(16);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(24));
        set.setBackground(bg);
        set.setOnClickListener(v -> applyWallpaper());
        LinearLayout.LayoutParams setLp = new LinearLayout.LayoutParams(-1, dp(52));
        setLp.topMargin = dp(12);
        root.addView(set, setLp);

        setContentView(root);
        select(selected);
    }

    private void select(int i) {
        selected = i;
        renderer.setStyle(i);
        Styles.save(this, i); // live wallpaper (if active) follows instantly
        title.setText(Styles.NAMES[i]);
        for (int k = 0; k < tiles.size(); k++) {
            GradientDrawable d = new GradientDrawable();
            d.setColor(Styles.COLORS[k]);
            d.setCornerRadius(dp(10));
            if (k == i) d.setStroke(dp(3), Color.WHITE);
            tiles.get(k).setBackground(d);
        }
    }

    private void applyWallpaper() {
        Styles.save(this, selected);
        try {
            Intent in = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            in.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this, ShaderWallpaperService.class));
            startActivity(in);
        } catch (Exception e) {
            try {
                startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
                Toast.makeText(this, "اختر Shader Walls من القائمة", Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                Toast.makeText(this, "الجهاز لا يدعم الخلفيات الحية", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        preview.onResume();
    }

    @Override
    protected void onPause() {
        preview.onPause();
        super.onPause();
    }
}
