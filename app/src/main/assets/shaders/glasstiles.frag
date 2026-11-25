void main() {
    vec2 uv = uvc(gl_FragCoord.xy);
    float n = 9.0;
    vec2 cell = floor(uv * n);
    vec2 lp = fract(uv * n) - 0.5;
    vec2 sp = (cell + 0.5) / n + lp * lp * lp * 0.9 / n;
    float v = sin(sp.x * 12.0 + u_time) + sin(sp.y * 9.0 - u_time * 0.7) + 2.0 * fbm(sp * 3.0 + u_time * 0.1);
    vec3 col = 0.5 + 0.5 * cos(v * 1.5 + vec3(0.0, 0.8, 2.0) + 3.5);
    col = mix(col, vec3(0.55, 0.2, 0.95), 0.35);
    float e = max(abs(lp.x), abs(lp.y));
    col += smoothstep(0.38, 0.5, e) * 0.35 * (lp.x + lp.y + 0.6);
    col *= 1.0 - smoothstep(0.47, 0.5, e) * 0.6;
    gl_FragColor = vec4(col, 1.0);
}
