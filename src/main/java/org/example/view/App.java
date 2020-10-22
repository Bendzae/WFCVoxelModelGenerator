package org.example.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.VoxelWfcModel;
import org.example.shared.VoxelWFCParameters;
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
  private BooleanProperty rotation = new SimpleBooleanProperty(true);
  private DoubleProperty avoidEmptyPattern = new SimpleDoubleProperty(0);
  private IntegerProperty patternSize = new SimpleIntegerProperty(2);
  private ObjectProperty<Vector3<Integer>> outputSize = new SimpleObjectProperty<>(new Vector3<>(15, 8, 15));
  private LongProperty rngSeed = new SimpleLongProperty((long) (Math.random() * 10000));
  private BooleanProperty useSeed = new SimpleBooleanProperty(false);

  int[][][] currentSolution = null;

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
    initExportButton(menu, stage);

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
    if (!this.useSeed.get()) {
      this.rngSeed.set((long) (Math.random() * 10000));
    }
    VoxelWfcModel voxelWfcModel = new VoxelWfcModel(
        inputArray,
        patternSize.get(),
        outputSize.get(),
        rotation.get(),
        avoidEmptyPattern.get(),
        this.rngSeed.get()
    );
    currentSolution = voxelWfcModel.solve();
    if (currentSolution != null) {
      voxelModelViewer.setModel(currentSolution);
    }
    saveParametersForModelToJSON();
  }

  private void showPatterns() {
    VoxelWfcModel voxelWfcModel = new VoxelWfcModel(
        inputArray,
        patternSize.get(),
        outputSize.get(),
        rotation.get(),
        avoidEmptyPattern.get(),
        rngSeed.get()
    );
    voxelModelViewer.showPatterns(voxelWfcModel, patternSize.get());
  }

  private void loadVoxModel(String filepath) {
    if (filepath == null) {
      return;
    }

    File paramsFile = new File(filepath.replace(".vox", "_params.json"));

    if (paramsFile.isFile()) {
      try {
        Gson gson = new Gson();
        JsonReader jsonReader = new JsonReader(new FileReader(paramsFile));
        VoxelWFCParameters voxelWFCParameters = gson.fromJson(jsonReader, VoxelWFCParameters.class);
        this.patternSize.setValue(voxelWFCParameters.patternSize);
        this.outputSize.setValue(new Vector3<>(
            voxelWFCParameters.outputSizeX,
            voxelWFCParameters.outputSizeY,
            voxelWFCParameters.outputSizeZ
        ));
        this.rotation.setValue(voxelWFCParameters.rotation);
        this.avoidEmptyPattern.setValue(voxelWFCParameters.avoidEmptyPattern);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
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

      modelFiles = walk.map(Path::toString)
          .filter(file -> file.endsWith(".vox"))
          .collect(Collectors.toList());
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
        outputSize.setValue(new Vector3<>(intValue, outputSize.get().getY(), outputSize.get().getZ()));
      }
    });
    outputSizeXTextField.setText(String.valueOf(outputSize.get().getX()));

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
        outputSize.setValue(new Vector3<>(outputSize.get().getX(), intValue, outputSize.get().getZ()));
      }
    });
    outputSizeYTextField.setText(String.valueOf(outputSize.get().getY()));

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
        outputSize.setValue(new Vector3<>(outputSize.get().getX(), outputSize.get().getY(), intValue));
      }
    });
    outputSizeZTextField.setText(String.valueOf(outputSize.get().getZ()));

    outputSizeHbox.getChildren().addAll(outputSizeXTextField, outputSizeYTextField, outputSizeZTextField);

    //Update textfield on value change
    outputSize.addListener((observable, oldValue, newValue) -> {
      outputSizeXTextField.setText(newValue.getX().toString());
      outputSizeYTextField.setText(newValue.getY().toString());
      outputSizeZTextField.setText(newValue.getZ().toString());
    });

    //Pattern Size
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
        patternSize.setValue(intValue);
      }
    });
    patternSizeTextField.setText(String.valueOf(patternSize.getValue()));

    //Update textfield on value change
    patternSize.addListener((observable, oldValue, newValue) -> {
      patternSizeTextField.setText(newValue.toString());
    });

    //Rotation
    CheckBox rotationCheckBox = new CheckBox("Rotation");
    Bindings.bindBidirectional(rotationCheckBox.selectedProperty(), this.rotation);

    //Avoid empty pattern
    Label avoidEmptyPatternLabel = new Label("Avoid Empty Pattern: " + avoidEmptyPattern.get());
    Slider avoidEmptyPatternSlider = new Slider(0, 0.99, avoidEmptyPattern.get());
    avoidEmptyPatternSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
      this.avoidEmptyPattern.setValue((double) newValue);
      avoidEmptyPatternLabel.setText(String.format("Avoid Empty Pattern: %.2f", avoidEmptyPattern.get()));
    });

    //Update slider on value change
    avoidEmptyPattern.addListener((observable, oldValue, newValue) -> {
      avoidEmptyPatternLabel.setText(String.format("Avoid Empty Pattern: %.2f", newValue));
      avoidEmptyPatternSlider.setValue((double) newValue);
    });

    //Seeding
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
    Bindings.bindBidirectional(seedCheckBox.selectedProperty(), this.useSeed);

    //Add to parent
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

  private void initExportButton(VBox parent, Stage stage) {
    Button exportButton = new Button("Export Model");

    exportButton.setOnAction(event -> {
      FileChooser fileChooser = new FileChooser();

      //Set extension filter for text files
      FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("VOX files (*.vox)", "*.vox");
      fileChooser.getExtensionFilters().add(extFilter);

      //Show save file dialog
      File file = fileChooser.showSaveDialog(stage);

      if (file != null && this.currentSolution != null) {
        //Export
        VoxSerializer voxSerializer = new VoxSerializer();
        voxSerializer.writeToVox(ModelConverter.arrayToVoxModel(this.currentSolution), voxelModelViewer.getPalette(), file);
      }
    });

    parent.getChildren().add(exportButton);
  }

  private void saveParametersForModelToJSON() {
    VoxelWFCParameters voxelWFCParameters = new VoxelWFCParameters(
        patternSize.get(),
        outputSize.get().getX(),
        outputSize.get().getY(),
        outputSize.get().getZ(),
        rotation.get(),
        avoidEmptyPattern.get()
    );
    Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    try {
      String currentInputFile = modelComboBox.getValue();
      String json = gson.toJson(voxelWFCParameters);
      BufferedWriter writer = new BufferedWriter(new FileWriter(currentInputFile.replace(".vox", "_params.json")));
      writer.write(json);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    launch();
  }
}
