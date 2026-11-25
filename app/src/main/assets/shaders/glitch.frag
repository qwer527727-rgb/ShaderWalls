void main() {
    vec2 uv = uvc(gl_FragCoord.xy);
    float bands = 40.0;
    float b = floor(uv.y * bands);
    float t = floor(u_time * 4.0);
    float off = (hash(vec2(b, t)) - 0.5) * 0.15 * step(0.7, hash(vec2(t, b * 0.3)));
    vec2 p = vec2(uv.x + off, uv.y) * 1.6;
    float v = warp(p, u_time * 0.5);
    vec3 col = 0.5 + 0.5 * cos(TAU * (v * 1.3 + vec3(0.30, 0.45, 0.65)));
    col = mix(col, vec3(0.35, 0.55, 0.30), smoothstep(0.55, 0.7, v) * 0.5);
    float blind = fract(uv.y * bands);
    col *= 0.7 + 0.3 * smoothstep(0.0, 0.5, blind);
    col.r = mix(col.r, 0.5 + 0.5 * cos(TAU * (warp(p + vec2(0.02, 0.0), u_time * 0.5) * 1.3 + 0.3)), 0.5);
    gl_FragColor = vec4(col, 1.0);
}
