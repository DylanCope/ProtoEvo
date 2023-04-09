varying vec4 v_color;
varying vec2 v_texCoord0;

uniform sampler2D u_sample2D;        // texture to blur
uniform float u_brightness;        // darken amount

void main(){
    gl_FragColor = u_brightness * texture2D(u_sample2D, v_texCoord0) * v_color;
}