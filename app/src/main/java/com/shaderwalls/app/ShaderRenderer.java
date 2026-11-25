package com.shaderwalls.app;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Draws one full-screen fragment shader. Each style is a .frag file in assets/shaders. */
public class ShaderRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "ShaderRenderer";
    private static final String VERT =
            "attribute vec2 a_pos;\nvoid main(){ gl_Position = vec4(a_pos, 0.0, 1.0); }\n";
    private static final long FRAME_MS = 33; // ~30 fps to save battery

    private final Context ctx;
    private volatile int requestedStyle;
    private int loadedStyle = -1;
    private int program;
    private int aPos, uRes, uTime, uScale, uBright, uMix, uPal;
    private int uSky, uCloud, uRain, uSnow, uFog, uStorm, uGlow;
    private final SkyModel sky = new SkyModel();
    private long lastSky;
    private volatile Styles.Settings settings = new Styles.Settings();
    private final float[] palBuf = new float[15];
    private float simTime;
    private long lastTick;
    private float width = 1, height = 1;
    private FloatBuffer quad;
    private long lastFrame;
    private String common, post;

    public ShaderRenderer(Context ctx, int style) {
        this.ctx = ctx.getApplicationContext();
        this.requestedStyle = style;
        this.settings = Styles.load(this.ctx, style);
    }

    public void setStyle(int style) {
        requestedStyle = style;
    }

    public void setSettings(Styles.Settings s) {
        if (s != null) settings = s;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        float[] v = {-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f};
        quad = ByteBuffer.allocateDirect(v.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        quad.put(v).position(0);
        loadedStyle = -1; // new GL context: rebuild program
        program = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        width = w;
        height = h;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = SystemClock.elapsedRealtime();
        long wait = (settings.paused ? 150 : FRAME_MS) - (now - lastFrame);
        if (wait > 0) {
            try { Thread.sleep(wait); } catch (InterruptedException ignored) { }
        }
        lastFrame = SystemClock.elapsedRealtime();

        int want = requestedStyle;
        if (want != loadedStyle) {
            if (program != 0) GLES20.glDeleteProgram(program);
            program = buildProgram(want);
            loadedStyle = want;
            if (program != 0) {
                aPos = GLES20.glGetAttribLocation(program, "a_pos");
                uRes = GLES20.glGetUniformLocation(program, "u_res");
                uTime = GLES20.glGetUniformLocation(program, "u_time");
                uScale = GLES20.glGetUniformLocation(program, "u_scale");
                uBright = GLES20.glGetUniformLocation(program, "u_bright");
                uMix = GLES20.glGetUniformLocation(program, "u_mix");
                uPal = GLES20.glGetUniformLocation(program, "u_pal[0]");
                uSky = GLES20.glGetUniformLocation(program, "u_sky[0]");
                uCloud = GLES20.glGetUniformLocation(program, "u_cloud");
                uRain = GLES20.glGetUniformLocation(program, "u_rain");
                uSnow = GLES20.glGetUniformLocation(program, "u_snow");
                uFog = GLES20.glGetUniformLocation(program, "u_fog");
                uStorm = GLES20.glGetUniformLocation(program, "u_storm");
                uGlow = GLES20.glGetUniformLocation(program, "u_glow");
                lastSky = 0;
            }
        }
        if (program == 0) {
            GLES20.glClearColor(0f, 0f, 0f, 1f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            return;
        }
        Styles.Settings st = settings;
        // advance animation clock by speed; wrap every hour to keep float precision
        long now2 = SystemClock.elapsedRealtime();
        if (lastTick == 0) lastTick = now2;
        float dt = Math.min(0.1f, (now2 - lastTick) / 1000f);
        lastTick = now2;
        simTime = (simTime + dt * (st.paused ? 0f : st.speed)) % 3600f;
        for (int i = 0; i < 5; i++) {
            int c = st.pal[i];
            palBuf[i * 3] = ((c >> 16) & 0xFF) / 255f;
            palBuf[i * 3 + 1] = ((c >> 8) & 0xFF) / 255f;
            palBuf[i * 3 + 2] = (c & 0xFF) / 255f;
        }
        GLES20.glUseProgram(program);
        GLES20.glUniform2f(uRes, width, height);
        GLES20.glUniform1f(uTime, simTime + 12f);
        GLES20.glUniform1f(uScale, st.scale);
        GLES20.glUniform1f(uBright, st.bright);
        GLES20.glUniform1f(uMix, st.mode == Styles.MODE_CUSTOM ? 1f : 0f);
        GLES20.glUniform3fv(uPal, 5, palBuf, 0);
        if (uSky >= 0) {
            if (now2 - lastSky > 2000) {
                SkyModel.compute(ctx, sky);
                lastSky = now2;
            }
            GLES20.glUniform3fv(uSky, 3, sky.sky, 0);
            GLES20.glUniform1f(uCloud, sky.cloud);
            GLES20.glUniform1f(uRain, sky.rain);
            GLES20.glUniform1f(uSnow, sky.snow);
            GLES20.glUniform1f(uFog, sky.fog);
            GLES20.glUniform1f(uStorm, sky.storm);
            GLES20.glUniform1f(uGlow, sky.glow);
        }
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, quad);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    private String asset(String path) {
        try (InputStream in = ctx.getAssets().open(path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "asset " + path, e);
            return "";
        }
    }

    private int buildProgram(int style) {
        if (common == null) common = asset("shaders/common.glsl");
        if (post == null) post = asset("shaders/post.glsl");
        String frag = common + "\n" + asset("shaders/" + Styles.FILES[style] + ".frag") + "\n" + post;
        int vs = compile(GLES20.GL_VERTEX_SHADER, VERT);
        int fs = compile(GLES20.GL_FRAGMENT_SHADER, frag);
        if (vs == 0 || fs == 0) return 0;
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, vs);
        GLES20.glAttachShader(p, fs);
        GLES20.glLinkProgram(p);
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) {
            Log.e(TAG, "link: " + GLES20.glGetProgramInfoLog(p));
            GLES20.glDeleteProgram(p);
            return 0;
        }
        return p;
    }

    private int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            Log.e(TAG, "compile: " + GLES20.glGetShaderInfoLog(s));
            GLES20.glDeleteShader(s);
            return 0;
        }
        return s;
    }
}
