void main() {
    vec2 uv = uvc(gl_FragCoord.xy);
    float v = warp(uv * 1.4, u_time * 0.5);
    float s = sin(v * 40.0);
    float line = pow(1.0 - abs(s), 8.0);
    float glow = pow(1.0 - abs(s), 2.0) * 0.15;
    float m = smoothstep(0.0, 0.6, clamp((v - 0.24) / 0.32, 0.0, 1.0));
    vec3 col = vec3(0.92, 0.95, 1.0) * (line + glow) * (0.25 + 0.75 * m);
    col += vec3(0.02, 0.02, 0.04);
    gl_FragColor = vec4(col, 1.0);
}
