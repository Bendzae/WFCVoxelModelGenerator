package org.example.model;

import java.util.Arrays;
import java.util.Objects;

public class Pattern3D {
    private int size;
    private int[][][] values;

    public Pattern3D(int size, int[][][] values) {
        this.size = size;
        this.values = values;
    }

    public Pattern3D(int size) {
        this.size = size;
        this.values = new int[size][size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    this.values[z][y][x] = -1;
                }
            }
        }
    }

    public void set(int x, int y, int z, int value) {
        values[z][y][x] = value;
    }

    public int get(int x, int y, int z) {
        return values[z][y][x];
    }

    public int getSize() {
        return size;
    }

    public int[][][] getRawArray() {
        return values;
    }

    /**
     * Clockwise 90 degree rotation around the X axis
     */
    public Pattern3D getXRotated() {
        int[][][] rotated = new int[size][size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    rotated[z][y][x] = get(x,size-z-1,y);
                }
            }
        }
        return new Pattern3D(size, rotated);
    }

    /**
     * Clockwise 90 degree rotation around the Z axis
     */
    public Pattern3D getZRotated() {
        int[][][] rotated = new int[size][size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    rotated[z][y][x] = get(y,size-x-1,z);
                }
            }
        }
        return new Pattern3D(size, rotated);
    }

    /**
     * Clockwise 90 degree rotation around the y axis
     */
    public Pattern3D getYRotated() {
        int[][][] rotated = new int[size][size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    rotated[z][y][x] = get(size-z-1,y,x);
                }
            }
        }
        return new Pattern3D(size, rotated);
    }



//    public int[] getEdge(Direction direction) {
//        int[] edge = new int[size];
//        switch (direction) {
//            case LEFT:
//                for (int y = 0; y < size; y++) {
//                    edge[y] = get(0, y);
//                }
//                break;
//            case RIGHT:
//                for (int y = 0; y < size; y++) {
//                    edge[y] = get(size - 1, y);
//                }
//                break;
//            case UP:
//                for (int x = 0; x < size; x++) {
//                    edge[x] = get(x, 0);
//                }
//                break;
//            case DOWN:
//                for (int x = 0; x < size; x++) {
//                    edge[x] = get(x, size - 1);
//                }
//                break;
//        }
//        return edge;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pattern3D pattern = (Pattern3D) o;
        if (size != pattern.size) return false;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    if(pattern.get(x,y,z) != values[z][y][x]) return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(size);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }

    @Override
    public String toString() {
        return "TODO";
    }
}
