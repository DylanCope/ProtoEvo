//varying vec4 v_color;
//varying vec2 v_texCoord0;
varying vec2 v_texCoords;

//uniform sampler2D sceneTex;
uniform sampler2D u_texture_pos; // 1
uniform sampler2D u_texture_vel; // 0
//uniform sampler2D u_sampler2D;
uniform mat4 u_projTrans;
uniform vec2 u_resolution;

const float decay = 0.95;
const float speedFactor = 1f;
const float blurFactor = 0.;


vec2 getVel(vec2 offset) {
    vec2 uv = v_texCoords + offset / u_resolution;
    vec2 velColor = texture2D(u_texture_vel, uv).xy;
    return (2.0 * velColor - 1.0);
}

vec4 getColour(vec2 offset) {
    vec2 uv = v_texCoords + offset / u_resolution;
    return texture2D(u_texture_pos, uv);
}

float speedInDirection(vec2 vel, vec2 dir) {
    float speed = dot(vel, normalize(dir));
    return clamp(speed, blurFactor, 1);
}

float getSpeedFromOffset(vec2 offset) {
    vec2 velAtOffset = getVel(offset);
    return speedInDirection(velAtOffset, -offset);
}

// builtins ref: https://registry.khronos.org/OpenGL-Refpages/gl4/index.php
void main() {
    vec4 color = getColour(vec2(0.0, 0.0));
    vec2 vel = 0.5 + 0.5 * getVel(vec2(0.0, 0.0));
//    color = decay * color;

//    float speed = length(getVel(vec2(0, 0)));
//    color = color * (1 - speed);


    vec4 newColor = vec4(0.0, 0.0, 0.0, 0.0);
    float totalSpeed = 0.0;

//    for (int i = -1; i <= 1; i += 1) {
//        for (int j = -1; j <= 1; j += 1) {
//            if (i == 0 && j == 0) {
//                continue;
//            }
//            vec2 offset = vec2(-1, -1);
//            vec2 offsetVel = getVel(offset);
//
//            float speedOut = speedInDirection(vel, offset);
//            float speedIn = speedInDirection(offsetVel, -offset);
//            newColor += vec4(speedIn, speedOut, 0.0, 1.0);
//        }
//    }
//    newColor /= totalSpeed;

    for (int i = -1; i <= 1; i += 1) {
        for (int j = -1; j <= 1; j += 1) {
            if (i == 0 && j == 0) {
                continue;
            }
            vec2 offset = vec2(i, j);
            vec2 offsetVel = getVel(offset);

            float speedOut = speedInDirection(vel, offset);
            float speedIn = speedInDirection(offsetVel, -offset);
//            newColor += vec4(speedOut, speedIn, 0.0, 0.0);
            float dotProduct = dot(vel, offsetVel);
            if (speedIn > speedOut) {
                newColor += mix(color, getColour(offset), speedIn - speedOut);
                totalSpeed += speedIn - speedOut;
            } else {
                newColor += mix(color, getColour(offset), speedOut - speedIn);
                totalSpeed += speedOut - speedIn;
            }
//                newColor += speed * getColour(offset);
//            float speedIn = getSpeedFromOffset(offset);
//            float speedOut = speedInDirection(vel, offset);
//            totalSpeed += speedIn - speedOut;
            // incoming flow: flow from offset pixel to centre
//            newColor += speedIn * getColour(offset);
            // outgoing flow: flow from centre to offset pixel
//            newColor -= speedOut * color;
        }
    }
//    newColor /= totalSpeed;

//    vec4 newColor = vec4(0, 0, 0, 0);
//    for (int i = -1; i <= 1; i += 1) {
//        for (int j = -1; j <= 1; j += 1) {
//            vec2 offset = vec2(i, j);
//            newColor += getColour(offset) / 9f;
//        }
//    }
    gl_FragColor = newColor;
}

//    vec4 color = vec4(0.0, 0.0, 0.0, 0.0);
//    for (int i = -1; i < 2; i++) {
//        for (int j = -1; j < 2; j++) {
//            vec2 offset = mul(vec4(i, j, 0, 0), u_projTrans).xy;
//            color += texture2D(u_texture_vel, v_texCoords + offset);
//        }
//    }
//    gl_FragColor = color / 9f;

//    vel = texture2D(u_texture_vel, v_texCoords).xy;
//    color = texture2D(u_texture_pos, v_texCoords + vel);
//    gl_FragColor = color;

//    color = texture2D(u_texture_vel, v_texCoords);
//    gl_FragColor = color;

//    vec2 offset = mul(vec4(vel.x, vel.y, 0, 0), u_projTrans).xy;
//    vec4 color = texture2D(u_texture_pos, v_texCoords + offset);
////    color = texture2D(u_texture_pos, v_texCoords);
//    gl_FragColor = color;