//varying vec4 v_color;
varying vec2 v_texCoord0;

uniform vec2 u_resolution;
//uniform sampler2D u_texture_pos;
uniform sampler2D u_sample2D;
uniform float u_delta;

const float decayFactor = 10.0;
const int n = 5;

void main() {
//    vec4 incomingColor = vec4(0.0, 0.0, 0.0, 0.0);
//    int k = (n - 1) / 2;
//    for (int i = -k; i <= k; i++) {
//        for (int j = -k; j <= k; j++) {
//            if (i == 0 && j == 0) {
//                continue;
//            }
//            vec2 uv = (gl_FragCoord.xy + vec2(i, j)) / u_resolution;
//            incomingColor += texture2D(u_texture_pos, uv);
//        }
//    }
//    incomingColor /= n*n - 1;
//
    vec2 uvCentre = gl_FragCoord.xy / u_resolution;
    vec4 color = texture2D(u_sample2D, uvCentre); // + u_delta * incomingColor;
//    gl_FragColor = (1 - u_delta * decayFactor) * color;

    gl_FragColor = color.xxxx;

//    vec4 color = texture2D(u_sampler2D, v_texCoord0);
    //    gl_FragColor = vec4(v_texCoord0, 0.0, 1.0);

//    vec4 color = vec4(0.0, 0.0, 0.0, 0.0);
//    for (int i = -1; i <= 1; i++) {
//        for (int j = -1; j <= 1; j++) {
//            vec2 uv = (gl_FragCoord.xy + vec2(i, j)) / u_resolution;
//            color += texture2D(u_texture_pos, uv);
//        }
//    }
//    color /= 9.0;
//    gl_FragColor = color;

//    gl_FragColor = texture2D(u_texture_pos, );
//    gl_FragColor = vec4(gl_FragCoord.xy, 0.0, 1.0);
}