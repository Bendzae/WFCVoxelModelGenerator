package org.example.ui;

import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
import org.example.voxparser.*;
import org.example.voxparser.VoxSerializer;
import org.example.wfc.NeighbourStrategy;
import org.example.wfc.OverlappingModel;
import org.example.wfc.SimpleModel3D;
import org.joml.Vector2i;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JavaFX App
 */
public class App extends Application {


    private enum ApplicationState {
        EDIT,
        VIEW;
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
    private int[] palette = ColorUtils.DEFAULT_PALETTE;

    //WFC Parameters
    private List<Color> colors = List.of(Color.WHITE, Color.RED, Color.BLACK, Color.BLUE, Color.GREEN, Color.YELLOW);
    private String currentFile;
    private int currentColor = 0;
    private boolean rotation = false;
    private boolean symmetry = false;
    private int patternSize = 2;
    private Vector3<Integer> inputSize = new Vector3<>(6, 6, 6);
    private Vector3<Integer> outputSize = new Vector3<>(30, 30, 30);
    private NeighbourStrategy neighbourStrategy = NeighbourStrategy.MATCH_EDGES;

    int[][][] inputArray;
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

        //Model Loader Label
        Label modelLabel = new Label("Input Model:");
        //Model Loader
        List<String> modelFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Paths.get("inputmodels"))) {

            modelFiles = walk.filter(Files::isRegularFile)
                    .map(Path::toString).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        final ComboBox<String> modelComboBox = new ComboBox<>(FXCollections.observableArrayList(modelFiles));
        modelComboBox.getSelectionModel().selectFirst();
        modelComboBox.setOnAction(actionEvent -> loadVoxModel(modelComboBox.getValue()));

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
                inputSize = new Vector3<>(intValue, intValue, intValue);
                if (applicationState == ApplicationState.EDIT) clearInput();
            }
        });
        inputSizeTextField.setText(String.valueOf(inputSize.getX()));

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
                outputSize = new Vector3<>(intValue, intValue, intValue);
            }
        });
        outputSizeTextField.setText(String.valueOf(outputSize.getX()));

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

        ObservableList<NeighbourStrategy> neighbourStrategies = FXCollections.observableArrayList(NeighbourStrategy.MATCH_EDGES, NeighbourStrategy.INPUT_NEIGHBOURS);
        final ComboBox<NeighbourStrategy> neighbourStrategyComboBox = new ComboBox<NeighbourStrategy>(neighbourStrategies);
        neighbourStrategyComboBox.getSelectionModel().selectFirst();
        neighbourStrategyComboBox.setOnAction(actionEvent -> neighbourStrategy = neighbourStrategyComboBox.getValue());


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
                modelComboBox,
                inputSizeLabel,
                inputSizeTextField,
                outputSizeLabel,
                outputSizeTextField,
                patternSizeLabel,
                patternSizeTextField,
                rotationCheckBox,
                symmetryCheckBox,
                neighbourStrategyComboBox,
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
//        initPatternEditor();
        loadVoxModel(modelComboBox.getValue());

        stage.setScene(scene);
        stage.show();
    }

    private void generate() {
        applicationState = ApplicationState.VIEW;
        boxes.getChildren().clear();
        SimpleModel3D simpleModel3D = new SimpleModel3D(inputArray, patternSize, outputSize, rotation, symmetry);

        simpleModel3D.patternsByPosition.forEach((pos, i) -> boxes.getChildren()
                .addAll(createBoxesFromVoxelArray(
                        simpleModel3D.patterns.get(i).getRawArray(),
                        new Vector3<>((pos.getX() * 2) * patternSize, (pos.getY() * 2) * patternSize, (pos.getZ() * 2) * patternSize + 1)
                        )
                ));
//        simpleModel3D.patternsByPosition.forEach((pos, i) -> boxes.getChildren()
//                .addAll(createBoxesFromVoxelArray(
//                        simpleModel3D.patterns.get(i).getRawArray(),
//                        new Vector3<>(pos.getX() * patternSize, pos.getY() * patternSize, pos.getZ() * patternSize)
//                        )
//                ));
    }

    private void loadVoxModel(String filepath) {
        if (boxes == null) return;
        inputArray = null;
        boxes.getChildren().clear();

        InputStream stream = null;
        try {
            stream = new FileInputStream(filepath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        VoxFile voxFile;
        voxFile = null;

        try (VoxReader reader = new VoxReader(stream)) {
            // VoxReader::read will never return null,
            // but it can throw an InvalidVoxException.
            voxFile = reader.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // VoxFile::getMaterials returns a map from material ID to material.
        // If your vox file contains the deprecated materials
        // stored in MATT chunks, use VoxFile::getOldMaterials instead.
        HashMap<Integer, VoxMaterial> materials = voxFile.getMaterials();

        // VoxFile::getPalette returns the palette used for the model.
        // The palette is an array of ints formatted as R8G8B8A8.
        this.palette = voxFile.getPalette();

        // VoxFile::getModels returns all the models used in the file.
        // Any valid .vox file must contain at least one model,
        // therefore models[0] will never be null.
        VoxModel[] models = voxFile.getModels();
        VoxModel model = models[0];

        // And finally, actually retrieving the voxels.
        Vector3<Integer> size = model.getSize();

        inputArray = model.to3DArray();

        Voxel[] voxels = model.getVoxels();

        double zoomedBoxSize = BOX_SIZE;

        this.boxes.getChildren().addAll(createBoxesFromVoxelArray(model.to3DArray()));

//        Arrays.stream(voxels).forEach(voxel -> {
//            byte x = voxel.getPosition().getX();
//            byte y = voxel.getPosition().getY();
//            byte z = voxel.getPosition().getZ();
//            int colourIndex = voxel.getColourIndex();
//            int color = colourIndex > 0 ? palette[colourIndex] : palette[colourIndex];
//
//            Box box = new Box(zoomedBoxSize, zoomedBoxSize, zoomedBoxSize);
//            box.translateXProperty().setValue(zoomedBoxSize * (x - (size.getX() / 2)));
//            box.translateYProperty().setValue(zoomedBoxSize * (-z - (-size.getZ() / 2)));
//            box.translateZProperty().setValue(zoomedBoxSize * (y - (size.getY() / 2)));
//
//            final PhongMaterial phongMaterial = new PhongMaterial();
//
//            phongMaterial.setDiffuseColor(ColorUtils.hexToColor(color));
//            box.setMaterial(phongMaterial);
//            box.setUserData(new Vector2i(x, y));
//            boxes.getChildren().add(box);
//        });
    }

    private void initPatternEditor() {
        applicationState = ApplicationState.EDIT;
        int inputX = inputSize.getX();
        int inputY = inputSize.getY();
        int inputZ = inputSize.getZ();

        if (boxes == null) return;

        boxes.getChildren().clear();
        if (inputArray == null) inputArray = new int[inputZ][inputY][inputX];

//        for (int x = 0; x < inputX; x++) {
//            for (int y = 0; y < inputY; y++) {
//                if (inputArray == null) inputArray[y][x] = 0;
//                Box box = new Box(zoomedBoxSize * 0.9f, zoomedBoxSize * 0.9f, zoomedBoxSize * 0.9f);
//                box.translateXProperty().setValue(zoomedBoxSize * (x - (inputX / 2)));
//                box.translateZProperty().setValue(0);
//                box.translateYProperty().setValue(zoomedBoxSize * (y - (inputY / 2)));
//                final PhongMaterial phongMaterial = new PhongMaterial();
//                phongMaterial.setDiffuseColor(colors.get(inputArray[y][x]));
//                box.setMaterial(phongMaterial);
//                box.setUserData(new Vector2i(x, y));
//                box.setOnMouseClicked(mouseEvent -> {
//                    final PhongMaterial material = (PhongMaterial) box.getMaterial();
//                    material.setDiffuseColor(colors.get(currentColor));
//                    box.setMaterial(material);
//                    Vector2i userData = (Vector2i) box.getUserData();
//                    inputArray[userData.y][userData.x] = currentColor;
//                });
//                boxes.getChildren().add(box);
//            }
//        }
    }

    private void clearInput() {
        inputArray = null;
        initPatternEditor();
    }

    private List<Box> createBoxesFromVoxelArray(int[][][] voxelModel) {
        return createBoxesFromVoxelArray(voxelModel, new Vector3<Integer>(0, 0, 0));
    }

    private List<Box> createBoxesFromVoxelArray(int[][][] voxelModel, Vector3<Integer> offset) {
        List<Box> result = new ArrayList<>();
        int sizeX = voxelModel[0][0].length;
        int sizeY = voxelModel[0].length;
        int sizeZ = voxelModel.length;
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    int colorIndex = voxelModel[z][y][x];
                    if (colorIndex > 0) {
                        Box box = new Box(BOX_SIZE, BOX_SIZE, BOX_SIZE);
                        box.translateXProperty().setValue(BOX_SIZE * (x + offset.getX() - (sizeX / 2)));
                        box.translateYProperty().setValue(BOX_SIZE * (y + offset.getY() - (sizeY / 2)));
                        box.translateZProperty().setValue(BOX_SIZE * (z + offset.getZ() - (sizeZ / 2)));
                        final PhongMaterial phongMaterial = new PhongMaterial();
                        phongMaterial.setDiffuseColor(ColorUtils.hexToColor(this.palette[colorIndex]));
                        box.setMaterial(phongMaterial);
                        result.add(box);
                    }
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

        scene.setOnScroll(scrollEvent -> {
            double delta = -scrollEvent.getDeltaY();
            //Add it to the Z-axis location.
            group.translateZProperty().set(group.getTranslateZ() + delta);
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