package com.shaderwalls.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

/** Live wallpaper that renders the selected shader style. */
public class ShaderWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new GLEngine();
    }

    class GLEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private WallpaperGLSurfaceView view;
        private ShaderRenderer renderer;

        /** GLSurfaceView that draws into the wallpaper's surface instead of its own. */
        class WallpaperGLSurfaceView extends GLSurfaceView {
            WallpaperGLSurfaceView(Context c) { super(c); }

            @Override
            public SurfaceHolder getHolder() { return getSurfaceHolder(); }

            void destroy() { super.onDetachedFromWindow(); }
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            view = new WallpaperGLSurfaceView(ShaderWallpaperService.this);
            view.setEGLContextClientVersion(2);
            view.setPreserveEGLContextOnPause(true);
            renderer = new ShaderRenderer(ShaderWallpaperService.this, Styles.current(ShaderWallpaperService.this));
            view.setRenderer(renderer);
            view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            Styles.prefs(ShaderWallpaperService.this).registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (view == null) return;
            if (visible) view.onResume(); else view.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences p, String key) {
            if (Styles.KEY_STYLE.equals(key) && renderer != null) {
                renderer.setStyle(Styles.current(ShaderWallpaperService.this));
            }
        }

        @Override
        public void onDestroy() {
            Styles.prefs(ShaderWallpaperService.this).unregisterOnSharedPreferenceChangeListener(this);
            if (view != null) view.destroy();
            super.onDestroy();
        }
    }
}
