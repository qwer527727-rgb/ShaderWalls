void main() {
    vec2 uv = uvc(gl_FragCoord.xy);
    float t = u_time * 0.15;
    vec3 col = vec3(0.92, 0.88, 0.96);
    vec2 p1 = vec2(0.4 * sin(t * 1.3), 0.6 * cos(t));
    vec2 p2 = vec2(0.45 * cos(t * 0.9 + 1.0), 0.5 * sin(t * 1.1 + 2.0));
    vec2 p3 = vec2(0.35 * sin(t * 0.7 + 4.0), 0.55 * cos(t * 1.4 + 3.0));
    col = mix(col, vec3(0.70, 0.62, 0.95), exp(-dot(uv - p1, uv - p1) * 4.0));
    col = mix(col, vec3(0.98, 0.70, 0.82), exp(-dot(uv - p2, uv - p2) * 5.0));
    col = mix(col, vec3(0.62, 0.85, 0.98), exp(-dot(uv - p3, uv - p3) * 5.0));
    col += (hash(gl_FragCoord.xy + fract(u_time)) - 0.5) * 0.02;
    gl_FragColor = vec4(col, 1.0);
}
