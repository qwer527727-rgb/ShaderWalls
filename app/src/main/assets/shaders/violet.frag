void main() {
    vec2 uv = uvc(gl_FragCoord.xy);
    vec3 vo = voronoi(uv * 4.0, u_time * 0.4);
    vec2 rp = uv + vec2(vo.x, vo.z) * 0.15;
    float v = warp(rp * 1.6, u_time * 0.4);
    vec3 col = mix(vec3(0.35, 0.20, 0.85), vec3(0.85, 0.75, 1.0), smoothstep(0.35, 0.75, v));
    col *= 0.7 + 0.5 * vo.x;
    col += smoothstep(0.06, 0.0, vo.y) * vec3(0.9, 0.85, 1.0) * 0.6;
    col += pow(max(0.0, 1.0 - vo.x * 2.5), 4.0) * 0.25;
    gl_FragColor = vec4(col, 1.0);
}
