void main() {
    vec2 uv = uvc(gl_FragCoord.xy);
    float t = u_time * 0.4;
    vec2 s1 = vec2(0.3 * sin(t), 0.5 * cos(t * 0.7));
    vec2 s2 = vec2(0.35 * cos(t * 0.9), -0.45 * sin(t * 0.6));
    vec2 s3 = vec2(-0.4 * sin(t * 0.5), 0.2 * cos(t * 1.1));
    float w = sin(length(uv - s1) * 40.0 - u_time * 2.0)
            + sin(length(uv - s2) * 40.0 - u_time * 2.0)
            + sin(length(uv - s3) * 35.0 - u_time * 1.5);
    float v = w / 3.0 * 0.5 + 0.5;
    vec3 col = mix(vec3(0.10, 0.20, 0.35), vec3(0.65, 0.80, 0.92), v);
    col = mix(col, vec3(0.95), smoothstep(0.85, 1.0, v) * 0.6);
    gl_FragColor = vec4(col, 1.0);
}
