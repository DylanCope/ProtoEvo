varying vec4 v_color;
varying vec2 v_texCoord0;

uniform sampler2D u_sample2D;        // texture to blur
uniform vec2 u_resolution;           // texture resolution
uniform float u_blurRadius;          // blur radius


void main(){
    float skipX = 0.3;
    float skipY = 0.3 * u_resolution.y / u_resolution.x;
    vec4 filter_color = vec4(0.);
    int filter_radius = 8;
    float sd = filter_radius * (skipX + skipY) / 2.0;
    float k = 1.0 / (2.0 * sd * sd);
    float sum = 0.0;
    for (float x = -filter_radius * skipX; x <= filter_radius * skipX; x += skipX) {
        for (float y = -filter_radius * skipY; y <= filter_radius * skipY; y += skipY) {
            float i = x + filter_radius;
            float j = y + filter_radius;
            float w = exp(-float(x*x + y*y) * k);
            sum += w;
            vec2 offset = vec2(x, y) / u_resolution;
            vec4 color = w * texture2D(u_sample2D, v_texCoord0 + offset) * v_color;
            filter_color += color;
        }
    }
    gl_FragColor = filter_color / sum;
}