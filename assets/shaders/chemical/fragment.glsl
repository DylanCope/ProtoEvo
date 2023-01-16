varying vec4 v_color;
varying vec2 v_texCoord0;

uniform sampler2D u_sample2D;        // texture to blur
uniform vec2 u_resolution;           // texture resolution
uniform float u_blurRadius;          // blur radius

void main()
{
    vec4 filter_color = vec4(0.);
    int filter_radius = 3; // int(u_blurRadius);
    for (int x = -filter_radius; x <= filter_radius; x++) {
        for (int y = -filter_radius; y <= filter_radius; y++) {
            vec2 offset = vec2(x, y) / u_resolution;
            vec4 color = texture2D(u_sample2D, v_texCoord0 + offset) * v_color;
            filter_color += color;
        }
    }
    float filter_size = float((filter_radius * 2 + 1) * (filter_radius * 2 + 1));
    gl_FragColor = filter_color / filter_size;
}