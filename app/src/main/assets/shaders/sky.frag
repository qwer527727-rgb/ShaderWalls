// Live Sky: grainy sunset-style gradient driven by real time of day and weather.
uniform vec3 u_sky[3];   // top, middle, bottom colours (computed on the CPU)
uniform float u_cloud;   // 0..1
uniform float u_rain;    // 0..1
uniform float u_snow;    // 0..1
uniform float u_fog;     // 0..1
uniform float u_storm;   // 0..1 lightning
uniform float u_glow;    // 0..1 sun glow near the horizon

void main() {
    vec2 fc = gl_FragCoord.xy;
    vec2 uv = fc / u_res;          // y = 1 at the top
    vec2 p = uvc(fc);
    float t = u_time;

    // slowly breathing gradient
    float w = fbm(p * 1.1 + vec2(t * 0.03, -t * 0.02)) - 0.5;
    float y = clamp(uv.y + w * 0.16, 0.0, 1.0);
    vec3 col = y > 0.5 ? mix(u_sky[1], u_sky[0], smoothstep(0.5, 1.0, y))
                       : mix(u_sky[2], u_sky[1], smoothstep(0.0, 0.5, y));

    // warm sun glow near the horizon
    vec2 g = (uv - vec2(0.5 + 0.08 * sin(t * 0.05), -0.08)) * vec2(u_res.x / u_res.y, 1.0);
    col += u_glow * vec3(1.0, 0.5, 0.28) * exp(-length(g) * 2.6) * 0.45;

    // drifting clouds
    if (u_cloud > 0.01) {
        vec2 cp = vec2(p.x * 1.6 + t * 0.035, p.y * 3.4);
        float c = fbm(cp + 0.8 * fbm(cp * 1.7 - t * 0.015));
        float cn = clamp((c - 0.28) / 0.36, 0.0, 1.0);
        float cov = smoothstep(1.0 - u_cloud * 0.9, 1.25 - u_cloud * 0.75, cn) * min(1.0, u_cloud * 1.6);
        float l = dot(col, vec3(0.299, 0.587, 0.114));
        vec3 ccol = mix(col, vec3(0.3 + 0.8 * l), 0.6);
        ccol *= 0.92 + 0.12 * smoothstep(0.5, 0.9, c);
        col = mix(col, ccol, cov * 0.8);
    }

    // fog
    if (u_fog > 0.01) {
        float l = dot(col, vec3(0.299, 0.587, 0.114));
        float f = 0.45 + 0.35 * fbm(vec2(p.x * 1.5 + t * 0.05, p.y * 3.0));
        col = mix(col, vec3(l * 0.6 + 0.35), u_fog * f);
    }

    // rain streaks (3 depth layers)
    if (u_rain > 0.01) {
        float rain = 0.0;
        for (int i = 0; i < 3; i++) {
            float fi = float(i);
            float sc = 30.0 + fi * 22.0;
            vec2 q = vec2(p.x * sc + p.y * sc * 0.12, p.y * sc * 0.1 + t * (2.6 + fi * 0.8));
            float column = floor(q.x);
            float off = hash(vec2(column, fi));
            float yy = fract(q.y * 0.5 + off * 7.0);
            float seg = floor(q.y * 0.5 + off * 7.0);
            float line = smoothstep(0.09, 0.0, abs(fract(q.x) - 0.5))
                       * smoothstep(0.0, 0.04, yy) * smoothstep(0.4, 0.0, yy)
                       * step(1.0 - 0.55 * u_rain, hash(vec2(column + 3.1, seg + fi)));
            rain += line * (0.55 - fi * 0.13);
        }
        col = mix(col, vec3(0.86, 0.9, 1.0), clamp(rain, 0.0, 1.0) * 0.55);
    }

    // snow flakes (3 depth layers)
    if (u_snow > 0.01) {
        float snow = 0.0;
        for (int i = 0; i < 3; i++) {
            float fi = float(i);
            float sc = 6.0 + fi * 5.0;
            vec2 q = p * sc;
            q.y += t * (0.35 + fi * 0.2);
            q.x += sin(q.y * 0.8 + fi * 2.0 + t * 0.4) * 0.35;
            vec2 id = floor(q);
            vec2 f = fract(q) - 0.5;
            vec2 o = vec2(hash(id + fi), hash(id + fi + 5.3)) - 0.5;
            float d = length(f - o * 0.6);
            float size = 0.05 + 0.06 * hash(id + 9.1);
            snow += smoothstep(size, size * 0.25, d) * step(1.0 - 0.7 * u_snow, hash(id + fi * 3.7));
        }
        col = mix(col, vec3(1.0), clamp(snow, 0.0, 1.0) * 0.9);
    }

    // lightning
    if (u_storm > 0.01) {
        float k = floor(t * 0.45);
        float ph = fract(t * 0.45);
        float flash = step(0.6, hash(vec2(k, 1.7))) * (exp(-ph * 14.0) + 0.6 * exp(-abs(ph - 0.12) * 40.0));
        col += u_storm * flash * vec3(0.75, 0.8, 1.0) * (0.5 + 0.5 * uv.y);
    }

    // film grain like the reference photos
    col += (hash(fc + fract(t * 7.0) * 100.0) - 0.5) * 0.05;
    gl_FragColor = vec4(col, 1.0);
}
