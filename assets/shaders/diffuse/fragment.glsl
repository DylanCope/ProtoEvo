varying vec4 v_color;
varying vec2 v_texCoords;

uniform vec2 u_resolution;
uniform sampler2D u_sampler2D;
uniform float u_delta;

const float decayFactor = 10f;
const int n = 5;

void main() {
    vec4 incomingColor = vec4(0.0, 0.0, 0.0, 0.0);
    int k = (n - 1) / 2;
    for (int i = -k / 2; i <= k; i++) {
        for (int j = -k; j <= k; j++) {
            if (i == 0 && j == 0) {
                continue;
            }
            vec2 offset = vec2(i, j) / u_resolution;
            incomingColor += texture2D(u_sampler2D, v_texCoords + offset);
        }
    }
    incomingColor /= n*n - 1;

    vec4 color = texture2D(u_sampler2D, v_texCoords) + u_delta * incomingColor;
    gl_FragColor = (1 - u_delta * decayFactor) * color;
}