package org.example;

import org.joml.Vector2i;

public class Grid {
    private int[][] grid;
    private final Vector2i size;

    public Grid(int[][] grid) {
        this.grid = grid;
        this.size = new Vector2i(grid[0].length, grid.length);
    }

    public Grid(Vector2i size) {
        this.size = size;
        this.grid = new int[size.y()][size.x()];
    }

    public Vector2i size() {
        return size;
    }

    public int get(Vector2i position) {
        return get(position.x(), position.y());
    }
    public int get(int x, int y) {
        checkBounds(x, y);
        return grid[y][x];
    }

    public int getWrapped(Vector2i position) {
        return getWrapped(position.x(), position.y());
    }

    public int getWrapped(int x, int y) {
        int wrappedX = x % size.x();
        int wrappedY = y % size.y();
        return grid[wrappedY][wrappedX];
    }

    public void set(Vector2i position, int value) {
        set(position.x(), position.y(), value);
    }
    public void set(int x, int y, int value) {
        checkBounds(x,y);
        grid[y][x] = value;
    }

    public Pattern getPatternAtPosition(Vector2i position, int patternSize) {
        Pattern pattern = new Pattern(patternSize);
        for (int px = 0; px < patternSize; px++) {
            for (int py = 0; py < patternSize; py++) {
                pattern.set(px, py, getWrapped(position.x() + px, position.y() + py));
            }
        }
        return pattern;
    }

    private void checkBounds(int x, int y) {
        if (x >= size.x() || x < 0 || y >= size.y() || y < 0) {
            throw new IllegalArgumentException("Coordinates out of bound.");
        }
    }

    @Override
    public String toString() {
        return Utils.print2DArray(grid);
    }
}
