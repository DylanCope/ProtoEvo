__device__ const int FILTER_SIZE = 3;

extern "C"
__global__ void kernel(
    unsigned int width,
    unsigned int height,
    unsigned int *img,
    unsigned int *filter,
    unsigned int *result)
{

    unsigned int x = blockIdx.x*blockDim.x + threadIdx.x;
    unsigned int y = blockIdx.y*blockDim.y + threadIdx.y;

    unsigned int sum = 0;
    sum = 0;
    for(int filterY=0; filterY < FILTER_SIZE; filterY++) {
        for(int filterX=0; filterX < FILTER_SIZE; filterX++) {
            sum += img[ ((y + filterY) * width) + x + filterX ] * filter[ (filterY * FILTER_SIZE) + filterX ];
        }
    }

    if(y + 1 < height && x + 1 < width) {
        result[((y + 1) * width) + x + 1] = sum / 16;
    }
}