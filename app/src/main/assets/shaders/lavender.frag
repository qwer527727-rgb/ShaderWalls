void main() {
    vec2 uv = uvc(gl_FragCoord.xy);
    float n = 26.0;
    vec2 g = uv * n;
    vec2 cell = floor(g);
    vec2 lp = fract(g);
    float v = warpN(uv * 1.5, u_time * 0.4);
    float over = mod(cell.x + cell.y, 2.0);
    float thread = over > 0.5 ? sin(lp.y * PI) : sin(lp.x * PI);
    float along = over > 0.5 ? lp.x : lp.y;
    vec3 a = vec3(0.42, 0.36, 0.62), b = vec3(0.80, 0.72, 0.95);
    vec3 col = mix(a, b, smoothstep(0.1, 0.8, v + 0.1 * sin(along * PI)));
    col *= 0.55 + 0.45 * thread;
    col *= 0.9 + 0.1 * sin(along * PI);
    gl_FragColor = vec4(col, 1.0);
}
