package org.example.model;

import org.example.shared.Vector3;

public class Grid3D {

    private int[][][] grid;
    private final Vector3<Integer> size;

    public Grid3D(int[][][] input) {
        this.grid = input;
        this.size = new Vector3<>(input[0][0].length, input[0].length, input.length);
    }
    public Grid3D(int[][][] input, int padding) {
        Vector3<Integer> inputSize = new Vector3<>(input[0][0].length, input[0].length, input.length);
        this.size = new Vector3<>(inputSize.getX() + padding * 2, inputSize.getY() + padding, inputSize.getZ() + padding * 2);
        this.grid = new int[size().getZ()][size().getY()][size().getX()];

        for (int px = 0; px < size().getX(); px++) {
            for (int py = 0; py < size().getY() ; py++) {
                for (int pz = 0; pz < size().getZ(); pz++) {
                    if(px < padding || px >= size().getX() - padding || py < padding || pz < padding || pz >= size().getZ() - padding) {
                        this.grid[pz][py][px] = -1;
                    } else {
                        this.grid[pz][py][px] = input[pz - padding][py-padding][px-padding];
                    }
                }
            }
        }
    }

    public Grid3D(Vector3<Integer> size) {
        this.size = size;
        this.grid = new int[size.getZ()][size.getY()][size.getX()];
    }

    public Vector3<Integer> size() {
        return size;
    }

    public int get(Vector3<Integer> position) {
        return get(position.getX(), position.getY(), position.getZ());
    }

    public int get(int x, int y, int z) {
        checkBounds(x, y, z);
        return grid[z][y][x];
    }

//    public int getWrapped(Vector2i position) {
//        return getWrapped(position.x(), position.y());
//    }

//    public int getWrapped(int x, int y) {
//
//        int wrappedX = x % size.x();
//        if (wrappedX < 0) wrappedX = size.x() + wrappedX;
//        int wrappedY = y % size.y();
//        if (wrappedY < 0) wrappedY = size.y() + wrappedY;
//        return grid[wrappedY][wrappedX];
//    }

    public void set(Vector3<Integer> position, int value) {
        set(position.getX(), position.getY(), position.getZ(), value);
    }

    public void set(int x, int y, int z, int value) {
        checkBounds(x, y, z);
        grid[z][y][x] = value;
    }

    public Pattern3D getPatternAtPosition(Vector3<Integer> position, int patternSize) {
        Pattern3D pattern = new Pattern3D(patternSize);
        for (int px = 0; px < patternSize; px++) {
            for (int py = 0; py < patternSize; py++) {
                for(int pz = 0; pz < patternSize; pz++) {
                    try {
                        pattern.set(px, py, pz, get(position.getX() + px, position.getY() + py, position.getZ() + pz));
                    } catch (IllegalArgumentException ex) {
//                        pattern.set(px, py, pz, -1); //TODO better way to handle this?
                        return null;
                    }
                }
            }
        }
        return pattern;
    }

    /**
     * Clockwise 90 degree rotation around the y axis
     */
    public Grid3D getYRotated() {
        Integer sizeX = size().getZ();
        Integer sizeY = size().getY();
        Integer sizeZ = size().getX();
        int[][][] rotated = new int[sizeZ][sizeY][sizeX];
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    rotated[z][y][x] = get(sizeZ-z-1,y,x);
                }
            }
        }
        return new Grid3D(rotated);
    }

    private void checkBounds(int x, int y, int z) {
        if (x >= size.getX() || x < 0 || y >= size.getY() || y < 0 || z >= size.getZ() || z < 0) {
            throw new IllegalArgumentException("Coordinates out of bound.");
        }
    }

    @Override
    public String toString() {
        return "TODO";
    }
}
