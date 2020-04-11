package org.example;

import org.joml.Vector2i;

import java.util.List;

public class Utils {

    public static final Vector2i LEFT = new Vector2i(-1, 0);
    public static final Vector2i RIGHT = new Vector2i(1, 0);
    public static final Vector2i UP = new Vector2i(0, -1);
    public static final Vector2i DOWN = new Vector2i(0, 1);

    public static final List<Vector2i> DIRECTIONS = List.of(LEFT, RIGHT, UP, DOWN);

    public static String print2DArray(int[][] array) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int col = 0; col < array.length; col++) {
            for (int row = 0; row < array[0].length; row++) {
                stringBuilder.append(array[col][row] + "  ");
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
