void main() {
    vec2 uv = uvc(gl_FragCoord.xy);
    vec2 p = uv * 5.0 + vec2(fbm(uv * 3.0 + u_time * 0.05), fbm(uv * 3.0 + 7.0)) * 0.8;
    vec3 v1 = voronoi(p, u_time * 0.3);
    vec3 v2 = voronoi(p * 2.3 + 3.0, u_time * 0.2);
    vec3 bg = mix(vec3(0.85, 0.70, 0.52), vec3(0.95, 0.85, 0.70), fbm(uv * 4.0));
    float l1 = smoothstep(0.05, 0.0, v1.y);
    float l2 = smoothstep(0.04, 0.0, v2.y) * 0.6;
    vec3 col = mix(bg, vec3(0.20, 0.25, 0.30), max(l1, l2));
    gl_FragColor = vec4(col, 1.0);
}
