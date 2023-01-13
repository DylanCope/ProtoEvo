varying vec4 v_color;
varying vec2 v_texCoord0;

uniform vec2 u_resolution;
uniform sampler2D u_sampler2D;
uniform int u_tracking;
uniform mat4 u_projTrans;
uniform mat4 u_projTransInv;
uniform float u_void_dist;
uniform vec3 u_cam_pos;

const float outerRadius = .75, innerRadius = 0.3, intensity = .65;

vec4 getFilteredColour(vec2 uv, float distFromWorldCentre) {
    vec4 color = texture2D(u_sampler2D, uv) * v_color;

    // overall vignette effect
    float width = u_resolution.x;
    float height = u_resolution.y;
    vec2 circlePos = (gl_FragCoord.xy - u_resolution / 2.) / height;
    float distance = length(circlePos);

    float alpha = smoothstep(outerRadius, innerRadius, distance);
    color.rgb = mix(color.rgb, color.rgb * alpha, intensity);

    // additional vignette effect for tracking
    if (u_tracking == 1) {
        float alpha = smoothstep(0.402, 0.4, distance);
        color.rgb = mix(color.rgb, color.rgb * alpha, .6);
    }

    // void effect
    vec2 worldPos = u_cam_pos.xy + (vec4(2 * uv - 1, 0, 0) * u_projTransInv).xy;
    float distFromCentre = length(worldPos);
    alpha = smoothstep(u_void_dist, u_void_dist * 0.75, distFromWorldCentre);
    color.rgb = mix(color.rgb, color.rgb * alpha, .25);

    return color;
}

void main() {
    vec2 worldPos = u_cam_pos.xy + (vec4(2 * v_texCoord0 - 1, 0, 0) * u_projTransInv).xy;
    float distFromCentre = length(worldPos);

    if (distFromCentre > 0.9 * u_void_dist) {
        float t = (distFromCentre - u_void_dist) / (u_void_dist * 0.25);
        int filter_radius = 3 + int(t * 5);
        vec4 filter_color = vec4(0.);
        for (int x = -filter_radius; x <= filter_radius; x++) {
            for (int y = -filter_radius; y <= filter_radius; y++) {
                vec2 offset = vec2(x, y) / u_resolution;
                filter_color += getFilteredColour(v_texCoord0 + offset, distFromCentre);
            }
        }
        float filter_size = float((filter_radius * 2 + 1) * (filter_radius * 2 + 1));
        gl_FragColor = filter_color / filter_size;
    }
    else {
        gl_FragColor = getFilteredColour(v_texCoord0, distFromCentre);
    }
}