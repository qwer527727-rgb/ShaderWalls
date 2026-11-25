void main() {
    float cs = u_scale * min(u_res.x, u_res.y) / 48.0;
    vec2 cell = floor(gl_FragCoord.xy / cs);
    vec2 lp = fract(gl_FragCoord.xy / cs) - 0.5;
    vec2 p = uvc((cell + 0.5) * cs);
    float b = smoothstep(0.1, 0.85, warpN(p * 2.0, u_time * 0.8));
    b *= 0.6 + 0.4 * hash(cell + floor(u_time * 6.0));
    float d = length(lp);
    float led = smoothstep(0.35, 0.25, d);
    float halo = smoothstep(0.6, 0.0, d) * 0.25;
    vec3 col = vec3(1.0, 0.97, 0.92) * b * (led + halo) + vec3(0.06) * led;
    gl_FragColor = vec4(col, 1.0);
}
