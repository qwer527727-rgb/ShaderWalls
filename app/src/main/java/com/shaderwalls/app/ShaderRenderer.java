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
    private int aPos, uRes, uTime;
    private float width = 1, height = 1;
    private FloatBuffer quad;
    private long lastFrame;
    private final long start = SystemClock.elapsedRealtime();
    private String common;

    public ShaderRenderer(Context ctx, int style) {
        this.ctx = ctx.getApplicationContext();
        this.requestedStyle = style;
    }

    public void setStyle(int style) {
        requestedStyle = style;
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
        long wait = FRAME_MS - (now - lastFrame);
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
            }
        }
        if (program == 0) {
            GLES20.glClearColor(0f, 0f, 0f, 1f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            return;
        }
        // wrap time every hour so float precision stays good
        float t = ((lastFrame - start) % 3_600_000L) / 1000f;
        GLES20.glUseProgram(program);
        GLES20.glUniform2f(uRes, width, height);
        GLES20.glUniform1f(uTime, t);
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
        String frag = common + "\n" + asset("shaders/" + Styles.FILES[style] + ".frag");
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
