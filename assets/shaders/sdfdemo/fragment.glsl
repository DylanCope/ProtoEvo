varying vec4 v_color;
varying vec2 v_texCoord0;

uniform vec2 u_resolution;
uniform sampler2D u_sampler2D;
uniform int u_tracking;
uniform mat4 u_projTrans;

void main() {
    gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
}