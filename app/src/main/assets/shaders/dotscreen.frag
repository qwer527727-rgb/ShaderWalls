void main() {
    vec2 uv = uvc(gl_FragCoord.xy);
    float v = warpN(uv * 1.8, u_time * 0.6);
    float a = 0.6;
    mat2 R = mat2(cos(a), -sin(a), sin(a), cos(a));
    vec2 g = R * uv * 55.0;
    vec2 lp = fract(g) - 0.5;
    float r = mix(0.12, 0.55, smoothstep(0.0, 0.9, v));
    float d = smoothstep(r, r - 0.08, length(lp));
    vec3 bg = mix(vec3(0.05, 0.15, 0.75), vec3(0.15, 0.35, 0.95), v);
    vec3 col = mix(bg, vec3(0.02, 0.04, 0.25), d);
    gl_FragColor = vec4(col, 1.0);
}
