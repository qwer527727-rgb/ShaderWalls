void main() {
    float cs = min(u_res.x, u_res.y) / 34.0;
    vec2 cell = floor(gl_FragCoord.xy / cs);
    vec2 lp = fract(gl_FragCoord.xy / cs) - 0.5;
    vec2 p = uvc((cell + 0.5) * cs) * 2.2;
    float v = warpN(p, u_time * 0.5);
    float r = mix(0.10, 0.48, smoothstep(0.0, 0.8, v));
    float d = length(lp);
    float dot_ = smoothstep(r, r - 0.06, d);
    vec3 bg = vec3(0.10, 0.04, 0.08);
    vec3 dc = mix(vec3(0.85, 0.45, 0.55), vec3(1.0, 0.92, 0.88), smoothstep(0.3, 0.8, v));
    gl_FragColor = vec4(mix(bg, dc, dot_), 1.0);
}
