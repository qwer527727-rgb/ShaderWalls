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
}
