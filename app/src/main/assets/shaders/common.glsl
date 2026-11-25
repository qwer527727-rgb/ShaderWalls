#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
uniform vec2 u_res;
uniform float u_time;
uniform float u_scale;   // 1 = default size

#define PI 3.14159265
#define TAU 6.2831853

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i), b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0)), d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}
float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.03 + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}
float warp(vec2 p, float t) {
    vec2 q = vec2(fbm(p + vec2(0.0, t * 0.10)), fbm(p + vec2(5.2, 1.3) - t * 0.08));
    vec2 r = vec2(fbm(p + 3.0 * q + vec2(1.7, 9.2) + t * 0.05), fbm(p + 3.0 * q + vec2(8.3, 2.8)));
    return fbm(p + 2.5 * r);
}
// centered coords, shortest side = 1
vec2 uvc(vec2 fc) { return (fc - 0.5 * u_res) / (min(u_res.x, u_res.y) * u_scale); }

// x: dist to nearest, y: edge distance, z: cell random id
vec3 voronoi(vec2 p, float t) {
    vec2 n = floor(p), f = fract(p);
    float md = 8.0, md2 = 8.0;
    vec2 id = vec2(0.0);
    for (int j = -1; j <= 1; j++)
    for (int i = -1; i <= 1; i++) {
        vec2 g = vec2(float(i), float(j));
        vec2 o = vec2(hash(n + g), hash(n + g + 17.3));
        o = 0.5 + 0.4 * sin(t + TAU * o);
        float d = length(g + o - f);
        if (d < md) { md2 = md; md = d; id = n + g; }
        else if (d < md2) { md2 = d; }
    }
    return vec3(md, md2 - md, hash(id));
}
// xy: local coords in hex cell, zw: cell id
vec4 hexGrid(vec2 p) {
    vec2 s = vec2(1.0, 1.7320508);
    vec2 h = 0.5 * s;
    vec2 a = mod(p, s) - h;
    vec2 b = mod(p - h, s) - h;
    vec2 gv = dot(a, a) < dot(b, b) ? a : b;
    return vec4(gv, p - gv);
}
// warp() normalised to roughly 0..1 (median ~0.32)
float warpN(vec2 p, float t) { return clamp((warp(p, t) - 0.24) / 0.32, 0.0, 1.0); }

#define main styleMain
