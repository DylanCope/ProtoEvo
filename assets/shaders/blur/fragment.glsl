varying vec4 v_color;
varying vec2 v_texCoord0;

uniform sampler2D u_sample2D;        // texture to blur
uniform vec2 u_resolution;           // texture resolution
uniform float u_blurRadius;          // blur radius


uniform float offset[3] = float[](0.0, 1.3846153846, 3.2307692308);
uniform float weight[3] = float[](0.2270270270, 0.3162162162, 0.0702702703);

void main(){
//    vec4 filter_color = vec4(0.);
//    int filter_radius = 3; // int(u_blurRadius);
//    for (int x = -filter_radius; x <= filter_radius; x++) {
//        for (int y = -filter_radius; y <= filter_radius; y++) {
//            int i = x + filter_radius;
//            int j = y + filter_radius;
//            float w = gauss[i][j];
//            vec2 offset = vec2(x, y) / u_resolution;
//            vec4 color = w * texture2D(u_sample2D, v_texCoord0 + offset) * v_color;
//            filter_color += color;
//        }
//    }
////    float filter_size = float((filter_radius * 2 + 1) * (filter_radius * 2 + 1));
////    gl_FragColor = filter_color / filter_size;
//    gl_FragColor = filter_color;

    // 2D Gaussian Blur
    // http://rastergrid.com/blog/2010/09/efficient-gaussian-blur-with-linear-sampling/

//    vec4 filter_color = texture2D(u_sample2D, v_texCoord0) * v_color * weight[0];
//    for (int i=1; i<3; i++) {
////        filter_color += 0.5 * weight[i] * texture2D(u_sample2D, v_texCoord0 + vec2(0.0, offset[i]) / u_resolution) * v_color;
////        filter_color += 0.5 * weight[i] * texture2D(u_sample2D, v_texCoord0 - vec2(0.0, offset[i]) / u_resolution) * v_color;
////        filter_color += 0.5 * weight[i] * texture2D(u_sample2D, v_texCoord0 + vec2(offset[i], 0.0) / u_resolution) * v_color;
////        filter_color += 0.5 * weight[i] * texture2D(u_sample2D, v_texCoord0 - vec2(offset[i], 0.0) / u_resolution) * v_color;
//        for (int j = 1; j < 3; j++) {
//            filter_color += 0.25 * weight[i] * texture2D(u_sample2D, v_texCoord0 + vec2(0.0, offset[i]) / u_resolution) * v_color;
//            filter_color += 0.25 * weight[i] * texture2D(u_sample2D, v_texCoord0 - vec2(0.0, offset[i]) / u_resolution) * v_color;
//            filter_color += 0.25 * weight[i] * texture2D(u_sample2D, v_texCoord0 + vec2(offset[j], 0.0) / u_resolution) * v_color;
//            filter_color += 0.25 * weight[i] * texture2D(u_sample2D, v_texCoord0 - vec2(offset[j], 0.0) / u_resolution) * v_color;
//        }
//    }
    float skip = 1;
    vec4 filter_color = vec4(0.);
    int filter_radius = 3; // int(u_blurRadius);
    float sd = filter_radius * skip;
    float k = 1.0 / (2.0 * sd * sd);
    float sum = 0.0;
    for (float x = -filter_radius * skip; x <= filter_radius * skip; x += skip) {
        for (float y = -filter_radius * skip; y <= filter_radius * skip; y += skip) {
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