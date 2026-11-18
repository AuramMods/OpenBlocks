#version 150

uniform sampler2D Sampler0;
out vec4 fragColor;

void main() {
    fragColor = vec4(texelFetch(Sampler0, ivec2(gl_FragCoord.xy), 0).rgb, 1.0);
}
