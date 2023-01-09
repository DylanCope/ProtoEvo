varying vec4 v_color;
varying vec2 v_texCoord0;

uniform vec2 u_resolution;
uniform sampler2D u_sampler2D;
uniform int u_tracking;
uniform mat4 u_projTrans;

const float outerRadius = .75, innerRadius = 0.3, intensity = .65;

void main() {
    vec4 color = texture2D(u_sampler2D, v_texCoord0) * v_color;

    float width = u_resolution.x;
    float height = u_resolution.y;
    vec2 circlePos = (gl_FragCoord.xy - u_resolution / 2.) / height;
    float distance = length(circlePos);

    float alpha = smoothstep(outerRadius, innerRadius, distance);
    color.rgb = mix(color.rgb, color.rgb * alpha, intensity);

    if (u_tracking == 1) {

        float alpha = smoothstep(0.402, 0.4, distance);
        color.rgb = mix(color.rgb, color.rgb * alpha, .6);
    }

    gl_FragColor = color;
}