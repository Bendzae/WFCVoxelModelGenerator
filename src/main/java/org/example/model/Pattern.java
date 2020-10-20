package org.example.model;

import java.util.Arrays;
import java.util.Objects;

public class Pattern {
    private int size;
    private int[][] values;

    public Pattern(int size, int[][] values) {
        this.size = size;
        this.values = values;
    }

    public Pattern(int size) {
        this.size = size;
        this.values = new int[size][size];
    }

    public void set(int x, int y, int value) {
        values[y][x] = value;
    }

    public int get(int x, int y) {
        return values[y][x];
    }

    public int getSize() {
        return size;
    }

    public int[] getEdge(Direction direction) {
        int[] edge = new int[size];
        switch (direction) {
            case LEFT:
                for (int y = 0; y < size; y++) {
                    edge[y] = get(0, y);
                }
                break;
            case RIGHT:
                for (int y = 0; y < size; y++) {
                    edge[y] = get(size - 1, y);
                }
                break;
            case UP:
                for (int x = 0; x < size; x++) {
                    edge[x] = get(x, 0);
                }
                break;
            case DOWN:
                for (int x = 0; x < size; x++) {
                    edge[x] = get(x, size - 1);
                }
                break;
        }
        return edge;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pattern pattern = (Pattern) o;
        if (size != pattern.size) return false;
        for (int i = 0; i < size; i++) {
            if (!Arrays.equals(values[i], pattern.values[i])) return false;
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
        return Utils.print2DArray(values);
    }
}
