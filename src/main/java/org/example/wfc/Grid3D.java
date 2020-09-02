package org.example.wfc;

import org.example.voxparser.Vector3;

import java.util.ArrayList;
import java.util.List;

public class Grid3D {
    private int[][][] grid;
    private final Vector3<Integer> size;

    public Grid3D(int[][][] grid) {
        this.grid = grid;
        this.size = new Vector3<>(grid[0][0].length, grid[0].length, grid.length);
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
                        pattern.set(px, py, pz, -1); //TODO better way to handle this?
                    }
                }
            }
        }
        return pattern;
    }

    public List<Pattern> getRotatedPatterns(Pattern pattern) {
        List<Pattern> rotatedPatterns = new ArrayList<>();
        int size = pattern.getSize();
        Pattern current = pattern;
        for (int i = 0; i < 3; i++) {
            Pattern rotated = new Pattern(size);
            for (int x = 0; x < size; ++x) {
                for (int y = 0; y < size; ++y) {
                    rotated.set(x, y, current.get(y, size - x - 1));
                }
            }
            current = rotated;
            rotatedPatterns.add(rotated);
        }
        return rotatedPatterns;
    }

    public List<Pattern> getReflectedPatterns(Pattern pattern) {
        List<Pattern> reflectedPatterns = new ArrayList<>();
        int size = pattern.getSize();
        Pattern reflectedY = new Pattern(size);
        for (int x = 0; x < size; ++x) {
            for (int y = 0; y < size; ++y) {
                reflectedY.set(x, y, pattern.get(x, size - y - 1));
            }
        }
        reflectedPatterns.add(reflectedY);

        Pattern reflectedX = new Pattern(size);
        for (int x = 0; x < size; ++x) {
            for (int y = 0; y < size; ++y) {
                reflectedX.set(x, y, pattern.get(size - x - 1, y));
            }
        }
        reflectedPatterns.add(reflectedX);
        return reflectedPatterns;
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
