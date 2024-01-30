varying vec4 v_color;
varying vec2 v_texCoord0;

uniform vec2 u_resolution;
uniform sampler2D u_sampler2D;

uniform vec3 u_cam_pos;
uniform float u_cam_zoom;
uniform mat4 u_projTrans;
uniform mat4 u_projTransInv;
uniform float u_smoothingK;

uniform int u_nCircles;
uniform float[16] u_centresX;
uniform float[16] u_centresY;
uniform float[16] u_radii;

uniform int u_nConnections;
uniform float[16*16] u_connections;


float circleSDF(vec2 pixel, vec2 centre, float radius) {
    return distance(centre, pixel) - radius;
}


float connectedCirclesSDF(vec2 pixel, vec2 centre1, float radius1, vec2 centre2, float radius2) {
    vec2 d = centre2 - centre1;
    float d2 = dot(d, d);
    if (d2 == 0.0) {
        return circleSDF(pixel, centre1, max(radius1, radius2));
    }
    float t = clamp(dot(pixel - centre1, d) / d2, 0.0, 1.0);
    float dist = distance(pixel, mix(centre1, centre2, t)) - mix(radius1, radius2, t);
    return dist;
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
    return vec4(t, t, t, t);
}


void main() {
    vec2 uvWorldSpace = u_cam_pos.xy + (vec4(2 * v_texCoord0 - 1, 0, 0) * u_projTransInv).xy;
    if (u_nCircles == 0) {
        return;
    }

//    vec2 centre1 = vec2(u_centresX[0], u_centresY[0]);
//    float radius1 = u_radii[0];
//
//    float shape = circleSDF(uvWorldSpace, centre1, radius1);
    float shape = 1000.0;
    if (u_nCircles > 1) {
        for (int i = 0; i < u_nConnections; i++) {
            int index1 = int(u_connections[2*i]);
            int index2 = int(u_connections[2*i + 1]);
            vec2 centre1 = vec2(u_centresX[index1], u_centresY[index1]);
            vec2 centre2 = vec2(u_centresX[index2], u_centresY[index2]);
            float radius1 = u_radii[index1];
            float radius2 = u_radii[index2];
            float connectedShape = connectedCirclesSDF(uvWorldSpace, centre1, radius1, centre2, radius2);
            shape = smoothMin(shape, connectedShape, u_smoothingK * min(radius1, radius2));
//            shape = unionSDF(shape, connectedShape);
        }

//        for (int i = 1; i < u_nCircles; i++) {
//            vec2 centre = vec2(u_centresX[i], u_centresY[i]);
//            float sdf = circleSDF(uvWorldSpace, centre, u_radii[i]);
//            shape = smoothMin(shape, sdf, 1.5f * u_radii[i]);
//        }
    }

    if (shape > -0.01 && shape < 0.0) {
        float outlineVal = 0.7f;
        gl_FragColor = vec4(outlineVal, outlineVal, outlineVal, 1.0);
    }
    else {
        gl_FragColor = draw(shape);
    }
}