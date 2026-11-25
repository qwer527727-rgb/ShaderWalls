#undef main
uniform float u_bright;  // brightness multiplier
uniform float u_mix;     // 0 = original colours, 1 = custom palette
uniform vec3 u_pal[5];   // palette, dark -> light

vec3 gradMap(float t) {
    t = clamp(t, 0.0, 1.0) * 4.0;
    vec3 c = mix(u_pal[0], u_pal[1], clamp(t, 0.0, 1.0));
    c = mix(c, u_pal[2], clamp(t - 1.0, 0.0, 1.0));
    c = mix(c, u_pal[3], clamp(t - 2.0, 0.0, 1.0));
    c = mix(c, u_pal[4], clamp(t - 3.0, 0.0, 1.0));
    return c;
}
void main() {
    styleMain();
    vec3 col = gl_FragColor.rgb;
    float l = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(col, gradMap(l), u_mix);
    gl_FragColor = vec4(clamp(col * u_bright, 0.0, 1.0), 1.0);
}
