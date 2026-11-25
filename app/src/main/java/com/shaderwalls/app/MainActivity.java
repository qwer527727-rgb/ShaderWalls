package com.shaderwalls.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_IMAGE = 41;
    private static final int BG = 0xFF0E0E12, PANEL = 0xFF1A1A22, ACCENT = 0xFF8C7CFF;

    private GLSurfaceView preview;
    private ShaderRenderer renderer;
    private TextView title;
    private Button editToggle;
    private ScrollView gridScroll, editScroll;
    private final List<View> tiles = new ArrayList<>();
    private int selected;
    private Styles.Settings cur;

    // editor widgets
    private SeekBar speedBar, scaleBar, brightBar;
    private TextView speedVal, scaleVal, brightVal;
    private final View[] swatches = new View[5];
    private TextView modeOriginal, modeCustom;
    private boolean updatingUi;

    private interface FloatSetter { void set(float v); }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        selected = Styles.current(this);
        cur = Styles.load(this, selected);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));

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
        topLp.topMargin = dp(10);
        topLp.bottomMargin = dp(10);
        root.addView(top, topLp);

        final FrameLayout phone = new FrameLayout(this);
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

        // Bottom area: style grid OR editor
        FrameLayout bottom = new FrameLayout(this);
        root.addView(bottom, new LinearLayout.LayoutParams(-1, dp(250)));
        gridScroll = new ScrollView(this);
        gridScroll.addView(buildGrid());
        bottom.addView(gridScroll, new FrameLayout.LayoutParams(-1, -1));
        editScroll = new ScrollView(this);
        editScroll.addView(buildEditor());
        editScroll.setVisibility(View.GONE);
        bottom.addView(editScroll, new FrameLayout.LayoutParams(-1, -1));

        // Buttons row
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(-1, dp(52));
        bLp.topMargin = dp(12);
        root.addView(buttons, bLp);

        editToggle = pillButton("⚙ تعديل", PANEL, Color.WHITE);
        editToggle.setOnClickListener(v -> toggleEditor());
        LinearLayout.LayoutParams e = new LinearLayout.LayoutParams(0, -1, 1f);
        e.rightMargin = dp(8);
        buttons.addView(editToggle, e);

        Button set = pillButton("تعيين كخلفية", Color.WHITE, Color.BLACK);
        set.setOnClickListener(v -> applyWallpaper());
        buttons.addView(set, new LinearLayout.LayoutParams(0, -1, 1.4f));

        setContentView(root);
        select(selected);
    }

    // ---------------- grid ----------------

    private View buildGrid() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row = null;
        for (int i = 0; i < Styles.FILES.length; i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, new LinearLayout.LayoutParams(-1, dp(52)));
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
        return grid;
    }

    // ---------------- editor ----------------

    private View buildEditor() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(12), dp(8), dp(12), dp(12));
        p.setBackground(round(PANEL, 16));

        speedVal = new TextView(this);
        speedBar = slider(p, "السرعة", speedVal, 0, 300, v -> cur.speed = v);
        scaleVal = new TextView(this);
        scaleBar = slider(p, "الحجم", scaleVal, 40, 250, v -> cur.scale = v);
        brightVal = new TextView(this);
        brightBar = slider(p, "السطوع", brightVal, 50, 160, v -> cur.bright = v);

        p.addView(label("الألوان"));
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modeOriginal = chip("الأصلية");
        modeOriginal.setOnClickListener(v -> { cur.mode = Styles.MODE_ORIGINAL; changed(); });
        modeCustom = chip("مخصصة");
        modeCustom.setOnClickListener(v -> { cur.mode = Styles.MODE_CUSTOM; changed(); });
        LinearLayout.LayoutParams m = new LinearLayout.LayoutParams(0, dp(38), 1f);
        m.setMargins(dp(3), 0, dp(3), 0);
        modes.addView(modeOriginal, m);
        modes.addView(modeCustom, new LinearLayout.LayoutParams(m));
        p.addView(modes);

        TextView hint = new TextView(this);
        hint.setText("اضغط على أي لون لتغييره");
        hint.setTextColor(0xFF9A9AAA);
        hint.setTextSize(12);
        hint.setPadding(dp(4), dp(10), dp(4), dp(4));
        p.addView(hint);

        LinearLayout sw = new LinearLayout(this);
        sw.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 5; i++) {
            View s = new View(this);
            final int idx = i;
            s.setOnClickListener(v -> editColor(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            sw.addView(s, lp);
            swatches[i] = s;
        }
        p.addView(sw);

        Button photo = pillButton("🖼  أخذ الألوان من صورة", ACCENT, Color.WHITE);
        photo.setOnClickListener(v -> pickImage());
        LinearLayout.LayoutParams ph = new LinearLayout.LayoutParams(-1, dp(46));
        ph.topMargin = dp(12);
        p.addView(photo, ph);

        p.addView(label("ألوان جاهزة"));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < Styles.PRESETS.length; i++) {
            TextView t = new TextView(this);
            t.setText(Styles.PRESET_NAMES[i]);
            t.setTextColor(Color.WHITE);
            t.setShadowLayer(4, 0, 1, Color.BLACK);
            t.setTextSize(12);
            t.setGravity(Gravity.CENTER);
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, Styles.PRESETS[i]);
            g.setCornerRadius(dp(10));
            t.setBackground(g);
            final int idx = i;
            t.setOnClickListener(v -> {
                cur.pal = Styles.PRESETS[idx].clone();
                cur.mode = Styles.MODE_CUSTOM;
                changed();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(96), dp(44));
            lp.setMargins(dp(3), 0, dp(3), 0);
            presets.addView(t, lp);
        }
        hs.addView(presets);
        p.addView(hs);

        Button reset = pillButton("↺  إعادة الضبط", 0xFF2A2A35, Color.WHITE);
        reset.setOnClickListener(v -> {
            Styles.reset(this, selected);
            cur = Styles.load(this, selected);
            changed();
        });
        LinearLayout.LayoutParams rs = new LinearLayout.LayoutParams(-1, dp(44));
        rs.topMargin = dp(14);
        p.addView(reset, rs);
        return p;
    }

    private SeekBar slider(LinearLayout parent, String name, TextView valView, int min, int max, FloatSetter setter) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView n = new TextView(this);
        n.setText(name);
        n.setTextColor(Color.WHITE);
        n.setTextSize(14);
        row.addView(n, new LinearLayout.LayoutParams(dp(62), -2));
        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        row.addView(bar, new LinearLayout.LayoutParams(0, dp(40), 1f));
        valView.setTextColor(0xFFB8B8C8);
        valView.setTextSize(13);
        valView.setGravity(Gravity.CENTER);
        row.addView(valView, new LinearLayout.LayoutParams(dp(50), -2));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                float v = (min + progress) / 100f;
                valView.setText(Math.round(v * 100) + "%");
                if (fromUser && !updatingUi) {
                    setter.set(v);
                    changed();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        bar.setTag(min);
        parent.addView(row);
        return bar;
    }

    private void setBar(SeekBar bar, float v) {
        int min = (Integer) bar.getTag();
        bar.setProgress(Math.round(v * 100) - min);
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(15);
        t.setPadding(dp(4), dp(14), dp(4), dp(8));
        return t;
    }

    private TextView chip(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(13);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private Button pillButton(String text, int bg, int fg) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(fg);
        b.setTextSize(15);
        b.setBackground(round(bg, 24));
        return b;
    }

    private GradientDrawable round(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private void refreshEditor() {
        updatingUi = true;
        setBar(speedBar, cur.speed);
        setBar(scaleBar, cur.scale);
        setBar(brightBar, cur.bright);
        updatingUi = false;
        boolean custom = cur.mode == Styles.MODE_CUSTOM;
        GradientDrawable on = round(ACCENT, 10), off = round(0xFF2A2A35, 10);
        modeOriginal.setBackground(custom ? off : on);
        modeCustom.setBackground(custom ? on : off);
        for (int i = 0; i < 5; i++) {
            GradientDrawable d = round(cur.pal[i], 10);
            d.setStroke(dp(1), 0x55FFFFFF);
            swatches[i].setBackground(d);
            swatches[i].setAlpha(custom ? 1f : 0.45f);
        }
    }

    /** Settings changed: save, push to preview (and live wallpaper via prefs), refresh UI. */
    private void changed() {
        Styles.saveSettings(this, selected, cur);
        renderer.setSettings(copy(cur));
        refreshEditor();
    }

    private static Styles.Settings copy(Styles.Settings s) {
        Styles.Settings c = new Styles.Settings();
        c.speed = s.speed;
        c.scale = s.scale;
        c.bright = s.bright;
        c.mode = s.mode;
        c.pal = s.pal.clone();
        return c;
    }

    private void toggleEditor() {
        boolean show = editScroll.getVisibility() != View.VISIBLE;
        editScroll.setVisibility(show ? View.VISIBLE : View.GONE);
        gridScroll.setVisibility(show ? View.GONE : View.VISIBLE);
        editToggle.setText(show ? "▦ الخلفيات" : "⚙ تعديل");
    }

    private void editColor(final int i) {
        final float[] hsv = new float[3];
        Color.colorToHSV(cur.pal[i], hsv);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20), dp(12), dp(20), dp(4));
        final View sample = new View(this);
        sample.setBackground(round(cur.pal[i], 12));
        l.addView(sample, new LinearLayout.LayoutParams(-1, dp(64)));
        String[] names = {"اللون", "التشبع", "الإضاءة"};
        int[] maxes = {360, 100, 100};
        for (int k = 0; k < 3; k++) {
            TextView n = new TextView(this);
            n.setText(names[k]);
            n.setPadding(0, dp(10), 0, 0);
            l.addView(n);
            SeekBar bar = new SeekBar(this);
            bar.setMax(maxes[k]);
            bar.setProgress(Math.round(k == 0 ? hsv[0] : hsv[k] * 100));
            final int kk = k;
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                    hsv[kk] = kk == 0 ? progress : progress / 100f;
                    sample.setBackground(round(Color.HSVToColor(hsv), 12));
                }
                @Override public void onStartTrackingTouch(SeekBar s) { }
                @Override public void onStopTrackingTouch(SeekBar s) { }
            });
            l.addView(bar);
        }
        new AlertDialog.Builder(this)
                .setTitle("اختر اللون")
                .setView(l)
                .setPositiveButton("تم", (d, which) -> {
                    cur.pal[i] = Color.HSVToColor(hsv);
                    cur.mode = Styles.MODE_CUSTOM;
                    changed();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void pickImage() {
        Intent in = new Intent(Intent.ACTION_GET_CONTENT);
        in.setType("image/*");
        in.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(in, "اختر صورة"), REQ_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "لا يوجد تطبيق صور", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_IMAGE || res != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        final int forStyle = selected;
        new Thread(() -> {
            try {
                final int[] pal = ColorExtract.fromUri(this, uri);
                runOnUiThread(() -> {
                    if (forStyle != selected) return;
                    cur.pal = pal;
                    cur.mode = Styles.MODE_CUSTOM;
                    changed();
                    Toast.makeText(this, "تم أخذ الألوان من الصورة ✓", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "تعذّر قراءة الصورة", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ---------------- selection / apply ----------------

    private void select(int i) {
        selected = i;
        cur = Styles.load(this, i);
        renderer.setSettings(copy(cur));
        renderer.setStyle(i);
        Styles.save(this, i); // live wallpaper (if active) follows instantly
        title.setText(Styles.NAMES[i]);
        for (int k = 0; k < tiles.size(); k++) {
            GradientDrawable d = round(Styles.COLORS[k], 10);
            if (k == i) d.setStroke(dp(3), Color.WHITE);
            tiles.get(k).setBackground(d);
        }
        refreshEditor();
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
