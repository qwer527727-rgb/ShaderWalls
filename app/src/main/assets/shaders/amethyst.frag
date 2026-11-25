void main() {
    vec2 p = uvc(gl_FragCoord.xy) * 7.0;
    vec3 v = voronoi(p, u_time * 0.5);
    float f = fbm(p * 0.3 + u_time * 0.05);
    vec3 a = vec3(0.22, 0.08, 0.40), b = vec3(0.62, 0.40, 0.92);
    vec3 col = mix(a, b, fract(v.z + f));
    col *= 0.75 + 0.5 * (1.0 - v.x);
    col = mix(vec3(0.07, 0.02, 0.12), col, smoothstep(0.02, 0.07, v.y));
    col += vec3(0.5, 0.35, 0.8) * smoothstep(0.08, 0.0, abs(v.y - 0.09)) * 0.25;
    gl_FragColor = vec4(col, 1.0);
}
