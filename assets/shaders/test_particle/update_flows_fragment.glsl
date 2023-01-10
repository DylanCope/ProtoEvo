//varying vec4 v_color;
//varying vec2 v_texCoord0;
varying vec2 v_texCoords;

//uniform sampler2D sceneTex;
//uniform sampler2D u_texture_pos; // 1
uniform sampler2D u_texture_vel; // 0
uniform vec2 u_resolution;
//uniform sampler2D u_sampler2D;
uniform mat4 u_projTrans;

vec2 getVel(vec2 offset) {
    vec2 uv = v_texCoords + offset / u_resolution;
    vec2 velColor = texture2D(u_texture_vel, uv).xy;
    return (2.0 * velColor - 1.0);
}

void main() {
    vec4 color = vec4(0.0, 0.0, 0.0, 0.0);
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            vec2 offset = mul(vec4(i, j, 0, 0), u_projTrans).xy;
            color += texture2D(u_texture_vel, v_texCoords + offset);
        }
    }
    gl_FragColor = color / 9f;
}