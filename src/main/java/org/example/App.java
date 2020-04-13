package org.example;

import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import org.joml.Vector2i;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * JavaFX App
 */
public class App extends Application {

    private static final int WIDTH = 700;
    private static final int HEIGHT = 700;
    private static final int BOX_SIZE = 5;

    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    private final DoubleProperty angleX = new SimpleDoubleProperty(0);
    private final DoubleProperty angleY = new SimpleDoubleProperty(0);
    private static Scene scene;
    private List<Color> colors = List.of(Color.WHITE, Color.RED, Color.BLACK, Color.BLUE);

    private int[][] input3 = new int[][]{
            {0, 0, 0, 0, 0},
            {0, 2, 2, 2, 0},
            {0, 2, 1, 2, 0},
            {0 ,2, 2, 2, 0},
            {0 ,0, 0, 0, 0},
    };

    @Override
    public void start(Stage stage) throws IOException {

        final int size = 20;

        Group root = new Group();
        SmartGroup boxes = new SmartGroup();

        OverlappingModel overlappingModel = new OverlappingModel(input3, 2, new Vector2i(50,50));

        boxes.getChildren().addAll(createBoxesFrom2DArray(overlappingModel.solve()));


        root.getChildren().add(boxes);

        Camera camera = new PerspectiveCamera();
        camera.translateXProperty().setValue(-WIDTH / 2);
        camera.translateYProperty().setValue(-HEIGHT / 2);
        camera.translateZProperty().setValue(WIDTH);
        scene = new Scene(root, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.BLACK.brighter().brighter());
        scene.setCamera(camera);

        initMouseControl(boxes, scene);

//        scene = new Scene(loadFXML("primary"));
        stage.setScene(scene);
        stage.show();
    }

    private List<Box> createBoxesFromVoxelArray(int[][][] voxelModel) {
        List<Box> result = new ArrayList<>();
        int size = voxelModel.length;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
//                    if (voxelModel[x][y][z] != 0) {
                        Box box = new Box(BOX_SIZE * 0.9f, BOX_SIZE * 0.9f, BOX_SIZE * 0.9f);
                        box.translateXProperty().setValue(BOX_SIZE * (x - (size/2)));
                        box.translateYProperty().setValue(BOX_SIZE * (y - (size/2)));
                        box.translateZProperty().setValue(BOX_SIZE * (z - (size/2)));
                        final PhongMaterial phongMaterial = new PhongMaterial();
                        phongMaterial.setDiffuseColor(Color.color(Math.random(), Math.random(), Math.random()));
                        box.setMaterial(phongMaterial);
                        result.add(box);
//                    }
                }
            }
        }
        return result;
    }

    private List<Box> createBoxesFrom2DArray(int[][] voxelModel) {
        List<Box> result = new ArrayList<>();
        int size = voxelModel.length;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
//                    if (voxelModel[y][x] != 0) {
                        Box box = new Box(BOX_SIZE * 0.9f, BOX_SIZE * 0.9f, BOX_SIZE * 0.9f);
                        box.translateXProperty().setValue(BOX_SIZE * (x - (size/2)));
                        box.translateZProperty().setValue(0);
                        box.translateYProperty().setValue(BOX_SIZE * (y - (size/2)));
                        final PhongMaterial phongMaterial = new PhongMaterial();
                        phongMaterial.setDiffuseColor(colors.get(voxelModel[y][x]));
                        box.setMaterial(phongMaterial);
                        result.add(box);
//                    }
            }
        }
        return result;
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    private void initMouseControl(SmartGroup group, Scene scene) {
        Rotate xRotate;
        Rotate yRotate;
        group.getTransforms().addAll(
                xRotate = new Rotate(0, Rotate.X_AXIS),
                yRotate = new Rotate(0, Rotate.Y_AXIS)
        );
        xRotate.angleProperty().bind(angleX);
        yRotate.angleProperty().bind(angleY);

        scene.setOnMousePressed(event -> {
            anchorX = event.getSceneX();
            anchorY = event.getSceneY();
            anchorAngleX = angleX.get();
            anchorAngleY = angleY.get();
        });

        scene.setOnMouseDragged(event -> {
            angleX.set(anchorAngleX - (anchorY - event.getSceneY()));
            angleY.set(anchorAngleY + anchorX - event.getSceneX());
        });
    }

    public static void main(String[] args) {
        launch();
    }

    class SmartGroup extends Group {

        Rotate r;
        Transform t = new Rotate();

        void rotateByX(int ang) {
            r = new Rotate(ang, Rotate.X_AXIS);
            t = t.createConcatenation(r);
            this.getTransforms().clear();
            this.getTransforms().addAll(t);
        }

        void rotateByY(int ang) {
            r = new Rotate(ang, Rotate.Y_AXIS);
            t = t.createConcatenation(r);
            this.getTransforms().clear();
            this.getTransforms().addAll(t);
        }
    }
}