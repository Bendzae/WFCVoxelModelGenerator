package org.example.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Application;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
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
import javafx.stage.Stage;
import org.example.model.VoxelWfcModel;
import org.example.voxparser.Vector3;
import org.example.voxparser.VoxSerializer;

/**
 * JavaFX App
 */
public class App extends Application {

  private static final int WIDTH = 1400;
  private static final int HEIGHT = 800;
  private static final int BOX_SIZE = 5;

  private static Scene scene;
  private VoxelModelViewer voxelModelViewer;
  private ComboBox<String> modelComboBox;

  //WFC Parameters
  int[][][] inputArray;
  private boolean rotation = true;
  private double avoidEmptyPattern = 0;
  private int patternSize = 2;
  private Vector3<Integer> inputSize = new Vector3<>(6, 6, 6);
  private Vector3<Integer> outputSize = new Vector3<>(15, 8, 15);
  private SimpleLongProperty rngSeed = new SimpleLongProperty((long) (Math.random() * 10000));
  private boolean useSeed = false;

  @Override
  public void start(Stage stage) throws IOException {
    //Root Element
    BorderPane parent = new BorderPane();

    //Left Menu
    VBox menu = new VBox();
    menu.setSpacing(10);
    menu.setPadding(new Insets(10));

    //Menu Elements
    initModelLoader(menu);
    initParameters(menu);
    initButtons(menu);

    parent.setLeft(menu);

    //Initialize Model Viewer
    this.voxelModelViewer = new VoxelModelViewer(WIDTH, HEIGHT, BOX_SIZE);
    parent.setRight(voxelModelViewer.getSubScene());
    parent.paddingProperty().setValue(new Insets(10d));
    scene = new Scene(parent, parent.getPrefWidth(), parent.getPrefHeight(), true, SceneAntialiasing.BALANCED);
    voxelModelViewer.initMouseControl(scene);

    //Initialize Inputs
    scene.setOnKeyPressed(keyEvent -> {
      System.out.println(keyEvent.getCode());
      if (keyEvent.getCode().equals(KeyCode.ENTER)) {
        generate();
      } else if (keyEvent.getCode().equals(KeyCode.BACK_SPACE)) {
        clearInput();
      }
    });

    //Load first model in Folder
    loadVoxModel(modelComboBox.getValue());

    //Show Scene
    stage.setScene(scene);
    stage.show();
  }

  private void generate() {
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
      voxelModelViewer.setModel(solution);

      //Export
      VoxSerializer voxSerializer = new VoxSerializer();
      voxSerializer.writeToVox(ModelConverter.arrayToVoxModel(solution), voxelModelViewer.getPalette(), "out.vox");
    }
  }

  private void showPatterns() {
    VoxelWfcModel voxelWfcModel = new VoxelWfcModel(
        inputArray,
        patternSize,
        outputSize,
        rotation,
        avoidEmptyPattern,
        rngSeed.get()
    );
    voxelModelViewer.showPatterns(voxelWfcModel, patternSize);
  }

  private void loadVoxModel(String filepath) {
    VoxelViewModel voxelViewModel = ModelConverter.loadVoxelModelFromFile(filepath);
    inputArray = voxelViewModel.getVoxelData();
    this.voxelModelViewer.setPalette(voxelViewModel.getPalette());
    this.voxelModelViewer.setModel(inputArray);
  }

  private void clearInput() {
    inputArray = null;
    this.voxelModelViewer.clear();
  }

  private void initModelLoader(VBox parent) {
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
    this.modelComboBox = new ComboBox<>(FXCollections.observableArrayList(modelFiles));
    modelComboBox.getSelectionModel().selectFirst();
    modelComboBox.setOnAction(actionEvent -> loadVoxModel(modelComboBox.getValue()));
    parent.getChildren().addAll(modelLabel, modelComboBox);
  }

  private void initParameters(VBox parent) {
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

    parent.getChildren().addAll(
        outputSizeLabel,
        outputSizeHbox,
        patternSizeLabel,
        patternSizeTextField,
        rotationCheckBox,
        avoidEmptyPatternLabel,
        avoidEmptyPatternSlider,
        seedLabel,
        seedTextField,
        seedCheckBox
    );
  }

  private void initButtons(VBox parent) {
    //Buttons
    Button generateButton = new Button("Generate");
    generateButton.setOnAction(actionEvent -> generate());
    Button showPatternsButton = new Button("Show Patterns");
    showPatternsButton.setOnAction(actionEvent -> showPatterns());
    Button clearInputButton = new Button("Clear");
    clearInputButton.setOnAction(actionEvent -> clearInput());

    parent.getChildren().addAll(generateButton, showPatternsButton, clearInputButton);
  }

  public static void main(String[] args) {
    launch();
  }
}
