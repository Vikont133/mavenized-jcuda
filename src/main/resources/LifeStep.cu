extern "C"
__global__ void lifeStep(char** lifeData, int width, int height) {

    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;

    int right = (x + 1) % width;
    int left = (x + width - 1) % width;

    int top = (y + height - 1) % height;
    int down = (y + 1) % height;

    // Count alive cells.
    int aliveCells =
        lifeData[left][top] +  lifeData[x][top]  + lifeData[right][top] +
        lifeData[left][y]                        + lifeData[right][y] +
        lifeData[left][down] + lifeData[x][down] + lifeData[right][down];

    lifeData[x][y] = aliveCells == 3 || (aliveCells == 2 && lifeData[x][y]) ? 1 : 0;
}