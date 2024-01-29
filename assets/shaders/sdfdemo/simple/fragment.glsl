varying vec4 v_color;
varying vec2 v_texCoord0;

uniform vec2 u_resolution;
uniform sampler2D u_sampler2D;
uniform int u_tracking;
uniform mat4 u_projTrans;
uniform float u_smoothingK;

uniform int u_nCircles;
uniform float[256] u_centresX;
uniform float[256] u_centresY;
uniform float[256] u_radii;


float circleSDF(vec2 pixel, vec2 centre, float radius) {
    return distance(centre, pixel) - radius;
}


float unionSDF(float sdf1, float sdf2) {
    return min(sdf1, sdf2);
}


float intersectSDF(float sdf1, float sdf2) {
    return max(sdf1, sdf2);
}


float subtractSDF(float sdf1, float sdf2) {
    return max(sdf1, -sdf2);
}

float smoothMin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}


vec4 draw(float shape) {
    float t = shape > 0.0 ? 0.0 : 1.0;
    return vec4(t, t, t, 1.0);
}


void main() {
    vec2 aspect = vec2(u_resolution.x / u_resolution.y, 1.0);
    vec2 pixel = v_texCoord0.xy * aspect;

    if (u_nCircles == 0) {
        return;
    }

    vec2 centre1 = vec2(u_centresX[0], u_centresY[0]);
    float radius1 = u_radii[0];

    float shape = circleSDF(pixel, centre1, radius1);

    if (u_nCircles > 1) {
        for (int i = 1; i < u_nCircles; i++) {
            vec2 centre = vec2(u_centresX[i], u_centresY[i]);
            float sdf = circleSDF(pixel, centre, u_radii[i]);
            shape = smoothMin(shape, sdf, u_smoothingK);
        }
    }

    gl_FragColor = draw(shape);
}