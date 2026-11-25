void main() {
    vec2 uv = uvc(gl_FragCoord.xy);
    float r = length(uv);
    float a = atan(uv.y, uv.x);
    float tw = a + 1.2 * sin(r * 3.0 - u_time * 0.6) + fbm(uv * 2.0 + u_time * 0.1);
    float rays = sin(tw * 14.0);
    vec3 cream = vec3(0.97, 0.94, 0.85), green = vec3(0.45, 0.62, 0.40), navy = vec3(0.12, 0.18, 0.35);
    vec3 col = mix(cream, green, smoothstep(-0.2, 0.4, rays));
    col = mix(col, navy, smoothstep(0.75, 0.95, rays));
    col = mix(col, cream, smoothstep(0.12, 0.0, r));
    gl_FragColor = vec4(col, 1.0);
}
