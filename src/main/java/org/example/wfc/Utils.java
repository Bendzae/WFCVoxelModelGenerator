package org.example.wfc;

import org.example.voxparser.Vector3;
import org.joml.Vector2i;

import java.util.List;

public class Utils {

    public static final Vector2i LEFT = new Vector2i(-1, 0);
    public static final Vector2i RIGHT = new Vector2i(1, 0);
    public static final Vector2i UP = new Vector2i(0, -1);
    public static final Vector2i DOWN = new Vector2i(0, 1);


    public static final Vector3<Integer> LEFT3D = new Vector3(-1, 0, 0);
    public static final Vector3<Integer> RIGHT3D = new Vector3(1, 0, 0);
    public static final Vector3<Integer> UP3D = new Vector3(0, -1, 0);
    public static final Vector3<Integer> DOWN3D = new Vector3(0, 1, 0);
    public static final Vector3<Integer> FORWARD3D = new Vector3(0, 0, 1);
    public static final Vector3<Integer> BACKWARD3D = new Vector3(0, 0, -1);

    public static final List<Vector2i> DIRECTIONS = List.of(LEFT, RIGHT, UP, DOWN);
    public static final List<Vector3<Integer>> DIRECTIONS3D = List.of(LEFT3D, RIGHT3D, UP3D, DOWN3D,FORWARD3D,BACKWARD3D);

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

    public static Direction3D rotateYDir(Direction3D direction3D) {
        switch (direction3D) {
            case LEFT:
                return Direction3D.FORWARD;
            case RIGHT:
                return Direction3D.BACKWARD;
            case FORWARD:
                return Direction3D.RIGHT;
            case BACKWARD:
                return Direction3D.LEFT;
            default:
                return direction3D;
        }
    }
}
