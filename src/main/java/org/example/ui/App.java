package org.example.ui;

import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import org.example.wfc.OverlappingModel;
import org.joml.Vector2i;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX App
 */
public class App extends Application {

    private enum ApplicationState {
        EDIT,
        VIEW
    }

    private ApplicationState applicationState = ApplicationState.EDIT;

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;
    private static final int BOX_SIZE = 5;

    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    private final DoubleProperty angleX = new SimpleDoubleProperty(0);
    private final DoubleProperty angleY = new SimpleDoubleProperty(0);
    private static Scene scene;

    //WFC Parameters
    private List<Color> colors = List.of(Color.WHITE, Color.RED, Color.BLACK, Color.BLUE);
    private int currentColor = 0;
    private boolean rotation = false;
    private boolean symmetry = false;
    private int patternSize = 2;
    private Vector2i inputSize = new Vector2i(5, 5);
    private Vector2i outputSize = new Vector2i(30, 30);

    int[][] inputArray;
    private SmartGroup boxes;

    @Override
    public void start(Stage stage) throws IOException {

        //Root Element
        BorderPane parent = new BorderPane();

        //Left Menu
        VBox menu = new VBox();
        menu.setSpacing(10);
        menu.setPadding(new Insets(10));

        //Color Label
        Label colorLabel = new Label("Color:");

        //Color Picker
        ObservableList<Color> options = FXCollections.observableArrayList(colors);
        final ComboBox<Color> comboBox = new ComboBox<Color>(options);
        comboBox.setCellFactory(c -> new ColorListCell());
        comboBox.setButtonCell(new ColorListCell());
        comboBox.getSelectionModel().selectFirst();
        comboBox.setOnAction(actionEvent -> currentColor = colors.indexOf(comboBox.getValue()));

        //Parameters
        Label inputSizeLabel = new Label("Input Size:");
        TextField inputSizeTextField = new TextField();
        int inputMaxSize = 9;
        inputSizeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d{0,10}")) {
                inputSizeTextField.setText(oldValue);
            } else {
                int intValue = Integer.parseInt(newValue);
                if (intValue > inputMaxSize) intValue = inputMaxSize;
                inputSizeTextField.setText(String.valueOf(intValue));
                inputSize = new Vector2i(intValue, intValue);
                if (applicationState == ApplicationState.EDIT) clearInput();
            }
        });
        inputSizeTextField.setText(String.valueOf(inputSize.x));

        Label outputSizeLabel = new Label("Output Size:");
        TextField outputSizeTextField = new TextField();
        int outputMaxSize = 100;
        outputSizeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d{0,10}")) {
                outputSizeTextField.setText(oldValue);
            } else {
                int intValue = Integer.parseInt(newValue);
                if (intValue > outputMaxSize) intValue = outputMaxSize;
                outputSizeTextField.setText(String.valueOf(intValue));
                outputSize = new Vector2i(intValue, intValue);
            }
        });
        outputSizeTextField.setText(String.valueOf(outputSize.x));

        Label patternSizeLabel = new Label("Pattern Size:");
        TextField patternSizeTextField = new TextField();
        int patternMaxSize = 10;
        patternSizeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d{0,10}")) {
                patternSizeTextField.setText(oldValue);
            } else {
                int intValue = Integer.parseInt(newValue);
                if (intValue > patternMaxSize) intValue = patternMaxSize;
                patternSizeTextField.setText(String.valueOf(intValue));
                patternSize = intValue;
            }
        });
        patternSizeTextField.setText(String.valueOf(patternSize));

        CheckBox rotationCheckBox = new CheckBox("Rotation");
        rotationCheckBox.setOnAction(actionEvent -> rotation = rotationCheckBox.isSelected());
        CheckBox symmetryCheckBox = new CheckBox("Symmetry");
        symmetryCheckBox.setOnAction(actionEvent -> symmetry = symmetryCheckBox.isSelected());

        //Buttons
        Button generateButton = new Button("Generate");
        generateButton.setOnAction(actionEvent -> generate());
        Button editInputButton = new Button("Edit Input");
        editInputButton.setOnAction(actionEvent -> initPatternEditor());
        Button clearInputButton = new Button("Clear");
        clearInputButton.setOnAction(actionEvent -> clearInput());

        menu.getChildren().addAll(
                colorLabel,
                comboBox,
                inputSizeLabel,
                inputSizeTextField,
                outputSizeLabel,
                outputSizeTextField,
                patternSizeLabel,
                patternSizeTextField,
                rotationCheckBox,
                symmetryCheckBox,
                generateButton,
                editInputButton,
                clearInputButton
        );

        parent.setLeft(menu);

        boxes = new SmartGroup();

        int w1 = WIDTH - 100;
        int h1 = HEIGHT;
        SubScene subScene = new SubScene(boxes, w1, h1, true, SceneAntialiasing.BALANCED);
        Camera camera = new PerspectiveCamera();
        camera.translateXProperty().setValue(-w1 / 2);
        camera.translateYProperty().setValue(-h1 / 2);
        camera.translateZProperty().setValue(w1);
        subScene.setFill(Color.BLACK.brighter().brighter());
        subScene.setCamera(camera);

        initPatternEditor();

        parent.setRight(subScene);

        scene = new Scene(parent, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);

        scene.setOnKeyPressed(keyEvent -> {
            System.out.println(keyEvent.getCode());
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                generate();
            } else if (keyEvent.getCode().equals(KeyCode.BACK_SPACE)) {
                initPatternEditor();
            } else if (keyEvent.getCode().equals(KeyCode.D)) {
                boxes.getChildren().clear();
            }
        });

        initMouseControl(boxes, scene);

        stage.setScene(scene);
        stage.show();
    }

    private void generate() {
        applicationState = ApplicationState.VIEW;
        boxes.getChildren().clear();
        OverlappingModel overlappingModel = new OverlappingModel(inputArray, patternSize, outputSize, rotation, symmetry);
        boxes.getChildren().addAll(createBoxesFrom2DArray(overlappingModel.solve()));
    }

    private void initPatternEditor() {
        applicationState = ApplicationState.EDIT;
        int inputX = inputSize.x;
        int inputY = inputSize.y;

        boxes.getChildren().clear();
        if (inputArray == null) inputArray = new int[inputY][inputX];

        double zoomedBoxSize = (BOX_SIZE * 5);

        for (int x = 0; x < inputX; x++) {
            for (int y = 0; y < inputY; y++) {
                if (inputArray == null) inputArray[y][x] = 0;
                Box box = new Box(zoomedBoxSize * 0.9f, zoomedBoxSize * 0.9f, zoomedBoxSize * 0.9f);
                box.translateXProperty().setValue(zoomedBoxSize * (x - (inputX / 2)));
                box.translateZProperty().setValue(0);
                box.translateYProperty().setValue(zoomedBoxSize * (y - (inputY / 2)));
                final PhongMaterial phongMaterial = new PhongMaterial();
                phongMaterial.setDiffuseColor(colors.get(inputArray[y][x]));
                box.setMaterial(phongMaterial);
                box.setUserData(new Vector2i(x, y));
                box.setOnMouseClicked(mouseEvent -> {
                    final PhongMaterial material = (PhongMaterial) box.getMaterial();
                    material.setDiffuseColor(colors.get(currentColor));
                    box.setMaterial(material);
                    Vector2i userData = (Vector2i) box.getUserData();
                    inputArray[userData.y][userData.x] = currentColor;
                });
                boxes.getChildren().add(box);
            }
        }
    }

    private void clearInput() {
        inputArray = null;
        initPatternEditor();
    }

    private List<Box> createBoxesFromVoxelArray(int[][][] voxelModel) {
        List<Box> result = new ArrayList<>();
        int size = voxelModel.length;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
//                    if (voxelModel[x][y][z] != 0) {
                    Box box = new Box(BOX_SIZE * 0.9f, BOX_SIZE * 0.9f, BOX_SIZE * 0.9f);
                    box.translateXProperty().setValue(BOX_SIZE * (x - (size / 2)));
                    box.translateYProperty().setValue(BOX_SIZE * (y - (size / 2)));
                    box.translateZProperty().setValue(BOX_SIZE * (z - (size / 2)));
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
                box.translateXProperty().setValue(BOX_SIZE * (x - (size / 2)));
                box.translateZProperty().setValue(0);
                box.translateYProperty().setValue(BOX_SIZE * (y - (size / 2)));
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