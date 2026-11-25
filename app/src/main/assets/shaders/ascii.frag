void main() {
    float sc = u_scale * min(u_res.x, u_res.y) / 60.0;
    vec2 cs = vec2(sc, sc * 1.6);
    vec2 cell = floor(gl_FragCoord.xy / cs);
    vec2 lp = fract(gl_FragCoord.xy / cs);
    vec2 p = uvc((cell + 0.5) * cs) * 2.0;
    float lum = smoothstep(0.05, 0.85, warpN(p, u_time * 0.7));
    vec2 g = floor(lp * vec2(5.0, 7.0));
    float inside = step(0.5, g.x) * step(g.x, 3.5) * step(0.5, g.y) * step(g.y, 5.5);
    float flick = floor(u_time * 3.0 + hash(cell) * 10.0);
    float on = step(hash(g + cell * 7.13 + flick), lum) * inside;
    vec3 col = vec3(0.25, 1.0, 0.45) * on * (0.4 + 0.8 * lum);
    col *= 0.85 + 0.15 * sin(gl_FragCoord.y * 1.5);
    col += vec3(0.0, 0.05, 0.02);
    gl_FragColor = vec4(col, 1.0);
}
