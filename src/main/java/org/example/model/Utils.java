package org.example.model;

import java.util.List;
import org.example.shared.Vector3;

public class Utils {

    public static final Vector3<Integer> LEFT = new Vector3(-1, 0, 0);
    public static final Vector3<Integer> RIGHT = new Vector3(1, 0, 0);
    public static final Vector3<Integer> UP = new Vector3(0, -1, 0);
    public static final Vector3<Integer> DOWN = new Vector3(0, 1, 0);
    public static final Vector3<Integer> FORWARD = new Vector3(0, 0, 1);
    public static final Vector3<Integer> BACKWARD = new Vector3(0, 0, -1);

    public static final List<Vector3<Integer>> DIRECTIONS = List.of(LEFT, RIGHT, UP, DOWN, FORWARD, BACKWARD);

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
    public static Direction3D opposite(Direction3D direction3D) {
        switch (direction3D) {
            case LEFT:
                return Direction3D.RIGHT;
            case RIGHT:
                return Direction3D.LEFT;
            case FORWARD:
                return Direction3D.BACKWARD;
            case BACKWARD:
                return Direction3D.FORWARD;
            case UP:
                return Direction3D.DOWN;
            case DOWN:
                return Direction3D.UP;
            default:
                return direction3D;
        }
    }

}
