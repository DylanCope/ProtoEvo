#version 430

// These need to match the workgroup size on Java side
layout(local_size_x = 8, local_size_y = 8) in;

const int FILTER_SIZE = 3;

layout(binding = 0, rgba8ui) uniform uimage2D outPixels;
layout(binding = 1, rgba8ui) uniform uimage2D pixels;

uniform int width;
uniform int height;
uniform int channels;

void main() {
    int x = int(gl_GlobalInvocationID.x);
    int y = int(gl_GlobalInvocationID.y);

    if (x > width || y > height) return;
    
    // See voidStartDistance in SimulationSettings
    float world_radius = 30.0;

    float cellSizeX = 2 * world_radius / float(width);
    float cellSizeY = 2 * world_radius / float(height);
    float world_x = -world_radius + cellSizeX * x;
    float world_y = -world_radius + cellSizeY * y;
    float dist2_to_world_centre = world_x*world_x + world_y*world_y;

    // set alpha decay to zero as we approach the void
    float decay = 0.0;

    float void_p = 0.9;
    if (dist2_to_world_centre > void_p * void_p * world_radius * world_radius) {
        float dist_to_world_centre = sqrt(dist2_to_world_centre);
        // lerp from 1.0 to 0.0 for distance between void_p*world_radius and world_radius
        decay = 0.9995 * (1.0 - (dist_to_world_centre - void_p * world_radius) / ((1.0 - void_p) * world_radius));
        if (decay < 0.0) {
            decay = 0.0;
        }
    } else {
        decay = 0.995;
    }

    int alpha_channel = channels - 1;
    uvec4 outp;

    float final_alpha = 0.0;
    int radius = (FILTER_SIZE - 1) / 2;
    for (int i = -radius; i <= radius; i++) {
        for (int j = -radius; j <= radius; j++) {
            int x_ = x + i;
            int y_ = y + j;
            if (x_ < 0 || x_ >= width || y_ < 0 || y_ >= height) {
                continue;
            }
            uvec4 pixel = imageLoad(pixels, ivec2(x_, y_));
            float val = float(pixel[alpha_channel]) / 255.0;
            final_alpha += val;
        }
    }
    final_alpha = decay * final_alpha / float(FILTER_SIZE*FILTER_SIZE);
    outp[alpha_channel] = int(255 * final_alpha);

    if (final_alpha < 5.0 / 255.0) {
        for (int i = 0; i < channels - 1; i++) {
            outp[i] = 0;
        }
    }

    float final_value = 0.0;
    // assume that the last channel is alpha
    for (int c = 0; c < channels - 1; c++) {
        final_value = 0.0;
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                int x_ = x + i;
                int y_ = y + j;
                if (x_ < 0 || x_ >= width || y_ < 0 || y_ >= height) {
                    continue;
                }
                uvec4 pixel = imageLoad(pixels, ivec2(x_, y_));
                float alpha = decay * float(pixel[alpha_channel]) / 255.0;
                float val = float(pixel[c]) / 255.0;
                final_value += val * alpha;
            }
        }
        final_value = final_value / float(FILTER_SIZE*FILTER_SIZE);
        final_value = decay * 255 * final_value / final_alpha;

        outp[c] = uint(int(final_value));
    }
    outp[3] = 255;
    
    imageStore(outPixels, ivec2(x, y), outp);
}