varying vec4 v_color;
varying vec2 v_texCoord0;

uniform vec2 u_resolution;
uniform sampler2D u_sampler2D;
uniform int u_tracking;
uniform mat4 u_projTrans;

const float outerRadius = .75, innerRadius = 0.2, intensity = .5;

void main() {
    vec4 color = texture2D(u_sampler2D, v_texCoord0) * v_color;

    vec2 relativePosition = gl_FragCoord.xy / u_resolution - 0.5;
    float distance = length(relativePosition);
    float alpha = smoothstep(outerRadius, innerRadius, distance);
    color.rgb = mix(color.rgb, color.rgb * alpha, intensity);

    if (u_tracking == 1) {
        float width = u_resolution.x;
        float height = u_resolution.y;
        vec2 circlePos = (gl_FragCoord.xy - u_resolution / 2.) / height;
        float distance = length(circlePos);

        float alpha = smoothstep(0.45, 0.448, distance);
        color.rgb = mix(color.rgb, color.rgb * alpha, .75);
    }

    gl_FragColor = color;
}