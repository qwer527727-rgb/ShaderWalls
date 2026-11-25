void main() {
    float cs = u_scale * min(u_res.x, u_res.y) / 24.0;
    vec2 cell = floor(gl_FragCoord.xy / cs);
    vec2 lp = fract(gl_FragCoord.xy / cs) - 0.5;
    vec2 p = uvc((cell + 0.5) * cs) * 1.0;
    float x = warp(p, u_time * 0.5) * 2.2 + 0.1;
    float v = abs(fract(x) * 2.0 - 1.0); // smooth there-and-back bands
    vec3 col;
    if (v < 0.16) col = vec3(0.27, 0.52, 0.86);
    else if (v < 0.32) col = vec3(0.60, 0.80, 0.96);
    else if (v < 0.58) col = vec3(0.98, 0.91, 0.76);
    else if (v < 0.70) col = vec3(0.97, 0.80, 0.58);
    else if (v < 0.80) col = vec3(0.93, 0.66, 0.38);
    else col = vec3(0.33, 0.60, 0.26);
    float d = length(lp);
    float stud = smoothstep(0.34, 0.30, d);
    float rim = smoothstep(0.30, 0.34, d) * smoothstep(0.40, 0.34, d);
    col *= 1.0 + stud * (lp.y - lp.x) * 0.35;
    col *= 1.0 - rim * 0.18;
    float edge = max(abs(lp.x), abs(lp.y));
    col *= 1.0 - smoothstep(0.44, 0.5, edge) * 0.25;
    gl_FragColor = vec4(col, 1.0);
}
