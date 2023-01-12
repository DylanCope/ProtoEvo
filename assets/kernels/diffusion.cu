__device__ const int FILTER_SIZE = 5;

extern "C"
__global__ void kernel(
    unsigned int width,
    unsigned int height,
    unsigned int channels,
    unsigned int *img,
    unsigned int *result)
{
    unsigned int x = blockIdx.x*blockDim.x + threadIdx.x;
    unsigned int y = blockIdx.y*blockDim.y + threadIdx.y;

    float decay = 0.9995;
    int alpha_channel = channels - 1;

    float final_alpha = 0.0;
    int radius = (FILTER_SIZE - 1) / 2;
    for (int i = -radius; i <= radius; i++) {
        for (int j = -radius; j <= radius; j++) {
            int x_ = x + i;
            int y_ = y + j;
            if (x_ < 0 || x_ >= width || y_ < 0 || y_ >= height) {
                continue;
            }
            final_alpha += ((float) img[(y_*width + x_)*channels + alpha_channel]) / 255.0;
        }
    }
    final_alpha = decay * final_alpha / ((float) (FILTER_SIZE*FILTER_SIZE));
    result[(y*width + x)*channels + alpha_channel] = (int) (255 * final_alpha);

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
                float alpha = decay * ((float) (img[(y_*width + x_)*channels + alpha_channel])) / 255.0;
                float val = ((float) img[(y_*width + x_)*channels + c]) / 255.0;
                final_value += val * alpha;
            }
        }
        final_value = final_value / ((float) (FILTER_SIZE*FILTER_SIZE));
        final_value = decay * 255 * final_value / final_alpha;
        result[(y*width + x)*channels + c] = (int) (final_value);
    }
}