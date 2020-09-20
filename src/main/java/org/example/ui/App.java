package org.example.ui;

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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import org.example.voxparser.Vector3;
import org.example.voxparser.VoxFile;
import org.example.voxparser.VoxModel;
import org.example.voxparser.VoxReader;
import org.example.wfc.Direction3D;
import org.example.wfc.NeighbourStrategy;
import org.example.wfc.SimpleModel3D;
import org.example.wfc.Tile3D;

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
  private Vector3<Integer> outputSize = new Vector3<>(3, 3, 3);
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
        if (intValue > inputMaxSize) {
          intValue = inputMaxSize;
        }
        inputSizeTextField.setText(String.valueOf(intValue));
        inputSize = new Vector3<>(intValue, intValue, intValue);
        if (applicationState == ApplicationState.EDIT) {
          clearInput();
        }
      }
    });
    inputSizeTextField.setText(String.valueOf(inputSize.getX()));

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
    rotationCheckBox.setOnAction(actionEvent -> rotation = rotationCheckBox.isSelected());
    CheckBox symmetryCheckBox = new CheckBox("Symmetry");
    symmetryCheckBox.setOnAction(actionEvent -> symmetry = symmetryCheckBox.isSelected());

    ObservableList<NeighbourStrategy> neighbourStrategies = FXCollections
        .observableArrayList(NeighbourStrategy.MATCH_EDGES, NeighbourStrategy.INPUT_NEIGHBOURS);
    final ComboBox<NeighbourStrategy> neighbourStrategyComboBox = new ComboBox<NeighbourStrategy>(neighbourStrategies);
    neighbourStrategyComboBox.getSelectionModel().selectFirst();
    neighbourStrategyComboBox.setOnAction(actionEvent -> neighbourStrategy = neighbourStrategyComboBox.getValue());

    //Buttons
    Button generateButton = new Button("Generate");
    generateButton.setOnAction(actionEvent -> generate());
    Button showPatternsButton = new Button("Show Patterns");
    showPatternsButton.setOnAction(actionEvent -> showPatterns());
    Button editInputButton = new Button("Edit Input");
    editInputButton.setOnAction(actionEvent -> initPatternEditor());
    Button clearInputButton = new Button("Clear");
    clearInputButton.setOnAction(actionEvent -> clearInput());
    Button generateWithTileset = new Button("Generate with Tileset");
    generateWithTileset.setOnAction(actionEvent -> generateWithTileset());

    menu.getChildren().addAll(
        colorLabel,
        comboBox,
        modelComboBox,
        inputSizeLabel,
        inputSizeTextField,
        outputSizeLabel,
        outputSizeHbox,
        patternSizeLabel,
        patternSizeTextField,
        rotationCheckBox,
        symmetryCheckBox,
        neighbourStrategyComboBox,
        generateButton,
        showPatternsButton,
        editInputButton,
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
    int[][][] solution = simpleModel3D.solve();
    if (solution != null) {
      boxes.getChildren().addAll(createBoxesFromVoxelArray(solution));
    }
  }

  private void generateWithTileset() {
    ArrayList<Tile3D> tiles = new ArrayList<>();

    Tile3D one = new Tile3D(2, loadVoxModelAs3DArray("inputmodels/tilesets/test/one.vox"));
    Tile3D two = new Tile3D(2, loadVoxModelAs3DArray("inputmodels/tilesets/test/two.vox"));

    Arrays.stream(Direction3D.values()).forEach(direction3D -> {
      one.addNeighbour(direction3D, 1);
      two.addNeighbour(direction3D, 0);
    });

    tiles.add(one);
    tiles.add(two);

    applicationState = ApplicationState.VIEW;
    boxes.getChildren().clear();

    SimpleModel3D simpleModel3D = new SimpleModel3D(tiles, patternSize, outputSize, rotation, symmetry);
    int[][][] solution = simpleModel3D.solve();
    if (solution != null) {
      boxes.getChildren().addAll(createBoxesFromVoxelArray(solution));
    }
  }

  private void showPatterns() {
    applicationState = ApplicationState.VIEW;
    boxes.getChildren().clear();
    SimpleModel3D simpleModel3D = new SimpleModel3D(inputArray, patternSize, outputSize, rotation, symmetry);

    simpleModel3D.patternsByPosition.forEach((pos, i) -> boxes.getChildren()
        .addAll(createBoxesFromVoxelArray(
            simpleModel3D.patterns.get(i).getRawArray(),
            new Vector3<>((pos.getX() * 2) * patternSize, (pos.getY() * 2) * patternSize, (pos.getZ() * 2) * patternSize)
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