void main() {
    vec2 uv = uvc(gl_FragCoord.xy);
    vec4 h = hexGrid(uv * 22.0);
    float v = warp(h.zw / 22.0 * 1.7, u_time * 0.5);
    vec3 c1 = vec3(0.08, 0.12, 0.30), c2 = vec3(0.95, 0.62, 0.20), c3 = vec3(0.98, 0.90, 0.70);
    vec3 col = v < 0.5 ? mix(c1, c2, smoothstep(0.42, 0.5, v)) : mix(c2, c3, smoothstep(0.58, 0.7, v));
    float d = length(h.xy);
    float bead = smoothstep(0.46, 0.40, d);
    col *= 0.6 + 0.6 * (1.0 - d * 1.6);
    col += pow(max(0.0, 1.0 - length(h.xy - vec2(-0.12, 0.14)) * 5.0), 3.0) * 0.6;
    col = mix(vec3(0.03, 0.03, 0.06), col, bead);
    gl_FragColor = vec4(col, 1.0);
}
