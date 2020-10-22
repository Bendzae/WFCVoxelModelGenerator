package org.example.view;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import org.example.model.Direction3D;
import org.example.model.Pattern3D;
import org.example.model.Tile3D;
import org.example.model.VoxelWfcModel;
import org.example.voxparser.Vector3;
import org.example.voxparser.VoxFile;
import org.example.voxparser.VoxModel;
import org.example.voxparser.VoxReader;
import org.example.voxparser.VoxSerializer;

/**
 * JavaFX App
 */
public class App extends Application {


  private enum ApplicationState {
    EDIT,
    VIEW;
  }

  private ApplicationState applicationState = ApplicationState.EDIT;

  private static final int WIDTH = 1400;
  private static final int HEIGHT = 800;
  private static final int BOX_SIZE = 5;

  private double anchorX, anchorY;
  private double anchorAngleX = 0;
  private double anchorAngleY = 0;
  private final DoubleProperty angleX = new SimpleDoubleProperty(0);
  private final DoubleProperty angleY = new SimpleDoubleProperty(0);
  private static Scene scene;
  private int[] palette = ColorUtils.DEFAULT_PALETTE;

  //WFC Parameters
  private boolean rotation = true;
  private double avoidEmptyPattern = 0;
  private int patternSize = 2;
  private Vector3<Integer> inputSize = new Vector3<>(6, 6, 6);
  private Vector3<Integer> outputSize = new Vector3<>(15, 8, 15);
  private SimpleLongProperty rngSeed = new SimpleLongProperty((long) (Math.random() * 10000));
  private boolean useSeed = false;

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
    Label outputSizeLabel = new Label("Output Size:");
    HBox outputSizeHbox = new HBox(3);
    int outputMaxSize = 100;
    int maxWidth = 50;
    TextField outputSizeXTextField = new TextField();
    outputSizeXTextField.setMaxWidth(maxWidth);
    outputSizeXTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.matches("\\d{0,10}")) {
        outputSizeXTextField.setText(oldValue);
      } else {
        int intValue = Integer.parseInt(newValue);
        if (intValue > outputMaxSize) {
          intValue = outputMaxSize;
        }
        outputSizeXTextField.setText(String.valueOf(intValue));
        outputSize = new Vector3<>(intValue, outputSize.getY(), outputSize.getZ());
      }
    });
    outputSizeXTextField.setText(String.valueOf(outputSize.getX()));

    TextField outputSizeYTextField = new TextField();
    outputSizeYTextField.setMaxWidth(maxWidth);
    outputSizeYTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.matches("\\d{0,10}")) {
        outputSizeYTextField.setText(oldValue);
      } else {
        int intValue = Integer.parseInt(newValue);
        if (intValue > outputMaxSize) {
          intValue = outputMaxSize;
        }
        outputSizeYTextField.setText(String.valueOf(intValue));
        outputSize = new Vector3<>(outputSize.getX(), intValue, outputSize.getZ());
      }
    });
    outputSizeYTextField.setText(String.valueOf(outputSize.getY()));

    TextField outputSizeZTextField = new TextField();
    outputSizeZTextField.setMaxWidth(maxWidth);
    outputSizeZTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.matches("\\d{0,10}")) {
        outputSizeZTextField.setText(oldValue);
      } else {
        int intValue = Integer.parseInt(newValue);
        if (intValue > outputMaxSize) {
          intValue = outputMaxSize;
        }
        outputSizeZTextField.setText(String.valueOf(intValue));
        outputSize = new Vector3<>(outputSize.getX(), outputSize.getY(), intValue);
      }
    });
    outputSizeZTextField.setText(String.valueOf(outputSize.getZ()));

    outputSizeHbox.getChildren().addAll(outputSizeXTextField, outputSizeYTextField, outputSizeZTextField);

    Label patternSizeLabel = new Label("Pattern Size:");
    TextField patternSizeTextField = new TextField();
    int patternMaxSize = 10;
    patternSizeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.matches("\\d{0,10}")) {
        patternSizeTextField.setText(oldValue);
      } else {
        int intValue = Integer.parseInt(newValue);
        if (intValue > patternMaxSize) {
          intValue = patternMaxSize;
        }
        patternSizeTextField.setText(String.valueOf(intValue));
        patternSize = intValue;
      }
    });
    patternSizeTextField.setText(String.valueOf(patternSize));

    CheckBox rotationCheckBox = new CheckBox("Rotation");
    rotationCheckBox.setSelected(rotation);
    rotationCheckBox.setOnAction(actionEvent -> rotation = rotationCheckBox.isSelected());

    Label avoidEmptyPatternLabel = new Label("Avoid Empty Pattern: " + avoidEmptyPattern);
    Slider avoidEmptyPatternSlider = new Slider(0, 0.99, avoidEmptyPattern);
    avoidEmptyPatternSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
      this.avoidEmptyPattern = (double) newValue;
      avoidEmptyPatternLabel.setText(String.format("Avoid Empty Pattern: %.2f", avoidEmptyPattern));
    });

    Label seedLabel = new Label("Current seed: " + rngSeed.get());
    TextField seedTextField = new TextField();
    seedTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.isEmpty()) {
        long longValue = Long.parseLong(newValue);
        this.rngSeed.set(longValue);
      }
    });

    rngSeed.addListener((observable, oldValue, newValue) -> {
      seedLabel.setText("Current seed: " + newValue.toString());
      seedTextField.setText(newValue.toString());
    });

    CheckBox seedCheckBox = new CheckBox("Use Seed");
    seedCheckBox.setSelected(this.useSeed);
    seedCheckBox.setOnAction(actionEvent -> this.useSeed = seedCheckBox.isSelected());

    //Buttons
    Button generateButton = new Button("Generate");
    generateButton.setOnAction(actionEvent -> generate());
    Button showPatternsButton = new Button("Show Patterns");
    showPatternsButton.setOnAction(actionEvent -> showPatterns());
    Button clearInputButton = new Button("Clear");
    clearInputButton.setOnAction(actionEvent -> clearInput());

    Button generateWithTileset = new Button("Generate with Tileset");
    generateWithTileset.setOnAction(actionEvent -> generateWithTileset());

    menu.getChildren().addAll(
        modelLabel,
        modelComboBox,
        outputSizeLabel,
        outputSizeHbox,
        patternSizeLabel,
        patternSizeTextField,
        rotationCheckBox,
        avoidEmptyPatternLabel,
        avoidEmptyPatternSlider,
        seedLabel,
        seedTextField,
        seedCheckBox,
        generateButton,
        showPatternsButton,
        clearInputButton,
        generateWithTileset
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
//    subScene.setFill(Color.WHITE);
    subScene.setCamera(camera);

    parent.setRight(subScene);
    parent.paddingProperty().setValue(new Insets(10d));

    scene = new Scene(parent, parent.getPrefWidth(), parent.getPrefHeight(), true, SceneAntialiasing.BALANCED);

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

    if (!this.useSeed) {
      this.rngSeed.set((long) (Math.random() * 10000));
    }
    VoxelWfcModel voxelWfcModel = new VoxelWfcModel(
        inputArray,
        patternSize,
        outputSize,
        rotation,
        avoidEmptyPattern,
        this.rngSeed.get()
    );
    int[][][] solution = voxelWfcModel.solve();
    if (solution != null) {
      boxes.getChildren().addAll(createBoxesFromVoxelArray(solution));


      VoxSerializer voxSerializer = new VoxSerializer();
      voxSerializer.writeToVox(new VoxModel(solution), palette, "out.vox");
    }
  }

  private void generateWithTileset() {
    ArrayList<Tile3D> tiles = new ArrayList<>();

    Tile3D zero = new Tile3D(2, new Pattern3D(2).getRawArray());
    Tile3D one = new Tile3D(2, loadVoxModelAs3DArray("inputmodels/tilesets/test/one.vox"));
    Tile3D two = new Tile3D(2, loadVoxModelAs3DArray("inputmodels/tilesets/test/two.vox"));

    Arrays.stream(Direction3D.values()).forEach(direction3D -> {
      one.addNeighbour(direction3D, 0);
      two.addNeighbour(direction3D, 0);
      zero.addNeighbour(direction3D, 0);
      zero.addNeighbour(direction3D, 1);
      zero.addNeighbour(direction3D, 2);
    });

    one.getNeighbours().neighbours.get(Direction3D.UP).clear();
    one.getNeighbours().neighbours.get(Direction3D.DOWN).clear();
    one.addNeighbour(Direction3D.UP, 2);

    one.addNeighbour(Direction3D.UP, 1);
    one.addNeighbour(Direction3D.DOWN, 1);

    one.addNeighbour(Direction3D.LEFT, 1);
    one.addNeighbour(Direction3D.RIGHT, 1);
    one.addNeighbour(Direction3D.FORWARD, 1);
    one.addNeighbour(Direction3D.BACKWARD, 1);

    two.getNeighbours().neighbours.get(Direction3D.DOWN).clear();
    two.addNeighbour(Direction3D.DOWN, 1);

    two.addNeighbour(Direction3D.UP, 2);
    two.addNeighbour(Direction3D.DOWN, 2);

    two.addNeighbour(Direction3D.LEFT, 2);
    two.addNeighbour(Direction3D.RIGHT, 2);
    two.addNeighbour(Direction3D.FORWARD, 2);
    two.addNeighbour(Direction3D.BACKWARD, 2);

    tiles.add(zero);
    tiles.add(one);
    tiles.add(two);

    applicationState = ApplicationState.VIEW;
    boxes.getChildren().clear();

    VoxelWfcModel voxelWfcModel = new VoxelWfcModel(tiles, patternSize, outputSize, rotation);
    int[][][] solution = voxelWfcModel.solve();
    if (solution != null) {
      boxes.getChildren().addAll(createBoxesFromVoxelArray(solution));
    }
  }

  private void showPatterns() {
    applicationState = ApplicationState.VIEW;
    boxes.getChildren().clear();
    VoxelWfcModel voxelWfcModel = new VoxelWfcModel(
        inputArray,
        patternSize,
        outputSize,
        rotation,
        avoidEmptyPattern,
        rngSeed.get()
    );

    voxelWfcModel.patternsByPosition.get(0).forEach((pos, i) -> boxes.getChildren()
        .addAll(createBoxesFromVoxelArray(
            voxelWfcModel.patterns.get(i).getRawArray(),
            new Vector3<>((pos.getX() * 2) * patternSize, (pos.getY() * 2) * patternSize, (pos.getZ() * 2) * patternSize),
            false
            )
        ));
  }

  private int[][][] loadVoxModelAs3DArray(String filepath) {
    InputStream stream = null;
    try {
      stream = new FileInputStream(filepath);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    VoxFile voxFile;
    voxFile = null;

    try (VoxReader reader = new VoxReader(stream)) {
      voxFile = reader.read();
    } catch (IOException e) {
      e.printStackTrace();
    }

    this.palette = voxFile.getPalette();

    VoxModel[] models = voxFile.getModels();
    VoxModel model = models[0];

    return model.to3DArray();
  }

  private void loadVoxModel(String filepath) {
    if (boxes == null) {
      return;
    }
    inputArray = loadVoxModelAs3DArray(filepath);
    boxes.getChildren().clear();

    this.boxes.getChildren().addAll(createBoxesFromVoxelArray(inputArray));
  }

  private void initPatternEditor() {
    applicationState = ApplicationState.EDIT;
    int inputX = inputSize.getX();
    int inputY = inputSize.getY();
    int inputZ = inputSize.getZ();

    if (boxes == null) {
      return;
    }

    boxes.getChildren().clear();
    if (inputArray == null) {
      inputArray = new int[inputZ][inputY][inputX];
    }
  }

  private void clearInput() {
    inputArray = null;
    initPatternEditor();
  }

  private List<Box> createBoxesFromVoxelArray(int[][][] voxelModel) {
    return createBoxesFromVoxelArray(voxelModel, new Vector3<Integer>(0, 0, 0), true);
  }

  private List<Box> createBoxesFromVoxelArray(int[][][] voxelModel, Vector3<Integer> offset, boolean floor) {
    List<Box> result = new ArrayList<>();
    int sizeX = voxelModel[0][0].length;
    int sizeY = voxelModel[0].length;
    int sizeZ = voxelModel.length;

    if (floor) {
      sizeY += 1;
    }

    for (int x = 0; x < sizeX; x++) {
      for (int y = 0; y < sizeY; y++) {
        for (int z = 0; z < sizeZ; z++) {
          int colorIndex = -1;
          if (floor && y == sizeY - 1) {
            colorIndex = 99;
          } else {
            colorIndex = voxelModel[z][y][x];
          }
          if (colorIndex >= 0) {
            Box box = new Box(BOX_SIZE, BOX_SIZE, BOX_SIZE);
            box.translateXProperty().setValue(BOX_SIZE * (x + offset.getX() - (sizeX / 2)));
            box.translateYProperty().setValue(BOX_SIZE * (y + offset.getY() - (sizeY / 2)));
            box.translateZProperty().setValue(BOX_SIZE * (z + offset.getZ() - (sizeZ / 2)));
            final PhongMaterial phongMaterial = new PhongMaterial();

            Color color = colorIndex == 99 ? new Color(0, 0, 0, 0.1) : ColorUtils.hexToColor(this.palette[colorIndex]);
            phongMaterial.setDiffuseColor(color);
            box.setMaterial(phongMaterial);
            result.add(box);
          }
        }
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