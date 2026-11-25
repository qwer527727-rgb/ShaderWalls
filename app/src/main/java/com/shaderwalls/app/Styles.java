package com.shaderwalls.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class Styles {
    private Styles() {}

    public static final String[] FILES = {
            "bricks", "ascii", "mosaic", "amethyst", "dotscreen", "glitch", "glasstiles", "liquid",
            "lavender", "hexbeads", "led", "sunburst", "violet", "interference", "aura", "lightweb"
    };
    public static final String[] NAMES = {
            "Bricks", "ASCII Terminal", "Mosaic", "Amethyst Tiles", "Dot Screen", "Glitch Blinds",
            "Glass Tiles", "Liquid Light", "Lavender Weave", "Hex Beads", "LED Matrix", "Sunburst",
            "Violet Glass", "Interference", "Aura", "Light Web"
    };
    /** Representative colour of each style, used for the picker buttons. */
    public static final int[] COLORS = {
            0xFFF2C98F, 0xFF0B1F10, 0xFF3A1528, 0xFF5B34A8, 0xFF1A3BD6, 0xFF4F8FCF,
            0xFF7A5BD9, 0xFF111216, 0xFF7E70A8, 0xFF1A2347, 0xFF1C1C1C, 0xFF6E9A62,
            0xFF5A3FD8, 0xFF5B7A99, 0xFFD8C8F0, 0xFFE0C49C
    };
    public static final boolean[] LIGHT = {
            true, false, false, false, false, false, false, false,
            false, false, false, false, false, false, true, true
    };

    static final String PREFS = "walls";
    static final String KEY_STYLE = "style";

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int current(Context c) {
        int s = prefs(c).getInt(KEY_STYLE, 0);
        return (s >= 0 && s < FILES.length) ? s : 0;
    }

    public static void save(Context c, int s) {
        prefs(c).edit().putInt(KEY_STYLE, s).apply();
    }

    // ---------- per-wallpaper settings ----------

    public static final int[] DEFAULT_PALETTE = {0xFF0B1D51, 0xFF725CAD, 0xFF8CCDEB, 0xFFFFE3A9, 0xFFFFFFFF};

    public static final String[] PRESET_NAMES = {"غروب", "محيط", "غابة", "نيون", "باستيل", "أبيض وأسود"};
    public static final int[][] PRESETS = {
            {0xFF2B1055, 0xFF7B2D7F, 0xFFE0475B, 0xFFF79D5C, 0xFFFFE6A7},
            {0xFF021B36, 0xFF0A4D8C, 0xFF1C9AD6, 0xFF7FD6F2, 0xFFE8FBFF},
            {0xFF0F2417, 0xFF2D5A27, 0xFF6B9E3F, 0xFFC5D86D, 0xFFF4F1DE},
            {0xFF0A0014, 0xFF5B00B5, 0xFFFF00A8, 0xFF00F0FF, 0xFFF6FF00},
            {0xFFB8A9E8, 0xFFF5B5CF, 0xFFFDE2C2, 0xFFBDE7E0, 0xFFFFFFFF},
            {0xFF000000, 0xFF333333, 0xFF777777, 0xFFBBBBBB, 0xFFFFFFFF},
    };

    public static final int MODE_ORIGINAL = 0, MODE_CUSTOM = 1;

    public static class Settings {
        public float speed = 1f, scale = 1f, bright = 1f;
        public int mode = MODE_ORIGINAL;
        public int[] pal = DEFAULT_PALETTE.clone();
    }

    public static Settings load(Context c, int s) {
        SharedPreferences p = prefs(c);
        Settings st = new Settings();
        st.speed = p.getFloat("speed_" + s, 1f);
        st.scale = p.getFloat("scale_" + s, 1f);
        st.bright = p.getFloat("bright_" + s, 1f);
        st.mode = p.getInt("mode_" + s, MODE_ORIGINAL);
        String pal = p.getString("pal_" + s, null);
        if (pal != null) {
            String[] parts = pal.split(",");
            if (parts.length == 5) {
                try {
                    for (int i = 0; i < 5; i++) st.pal[i] = (int) Long.parseLong(parts[i]);
                } catch (NumberFormatException ignored) { }
            }
        }
        return st;
    }

    public static void saveSettings(Context c, int s, Settings st) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i > 0) sb.append(',');
            sb.append(st.pal[i] & 0xFFFFFFFFL);
        }
        prefs(c).edit()
                .putFloat("speed_" + s, st.speed)
                .putFloat("scale_" + s, st.scale)
                .putFloat("bright_" + s, st.bright)
                .putInt("mode_" + s, st.mode)
                .putString("pal_" + s, sb.toString())
                .apply();
    }

    public static void reset(Context c, int s) {
        prefs(c).edit().remove("speed_" + s).remove("scale_" + s).remove("bright_" + s)
                .remove("mode_" + s).remove("pal_" + s).apply();
    }
}
