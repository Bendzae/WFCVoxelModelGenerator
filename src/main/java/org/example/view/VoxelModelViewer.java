package org.example.view;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.example.model.VoxelWfcModel;
import org.example.shared.Vector3;

public class VoxelModelViewer {

  private SmartGroup boxes;

  private SubScene subScene;

  private int boxSize;
  private double anchorX, anchorY;
  private int[] palette = ColorUtils.DEFAULT_PALETTE;

  private double anchorAngleX = 0;
  private double anchorAngleY = 0;
  private final DoubleProperty angleX = new SimpleDoubleProperty(0);
  private final DoubleProperty angleY = new SimpleDoubleProperty(0);

  public VoxelModelViewer(int width, int height, int boxSize, Color bgColor) {
    this.boxSize = boxSize;
    this.boxes = new SmartGroup();

    int w1 = width - 100;
    int h1 = height;
    this.subScene = new SubScene(boxes, w1, h1, true, SceneAntialiasing.BALANCED);
    Camera camera = new PerspectiveCamera();
    camera.translateXProperty().setValue(-w1 / 2);
    camera.translateYProperty().setValue(-h1 / 2);
    camera.translateZProperty().setValue(w1);
    subScene.setFill(bgColor);
    subScene.setCamera(camera);
  }

  public void initMouseControl(Scene scene) {
    Rotate xRotate;
    Rotate yRotate;
    this.boxes.getTransforms().addAll(
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
      this.boxes.translateZProperty().set(this.boxes.getTranslateZ() + delta);
    });
  }

  public SubScene getSubScene() {
    return subScene;
  }

  public void setModel(int[][][] voxelModel) {
    boxes.getChildren().clear();
    boxes.getChildren().addAll(createBoxesFromVoxelArray(voxelModel));
  }

  public int[] getPalette() {
    return this.palette;
  }

  public void setPalette(int[] palette) {
    this.palette = palette;
  }

  public void showPatterns(VoxelWfcModel voxelWfcModel, int patternSize) {
    boxes.getChildren().clear();
    voxelWfcModel.getPatternsByPosition().get(0).forEach((pos, i) -> boxes.getChildren()
        .addAll(createBoxesFromVoxelArray(
            voxelWfcModel.getPatterns().get(i).getRawArray(),
            new Vector3<>(
                (pos.getX() * 2) * patternSize - (voxelWfcModel.getInputSize().getX() * 2 / (patternSize)),
                (pos.getY() * 2) * patternSize - (voxelWfcModel.getInputSize().getY() * 2 / (patternSize)),
                (pos.getZ() * 2) * patternSize - (voxelWfcModel.getInputSize().getZ() * 2 / (patternSize))
            ),
            false
            )
        ));
  }

  public void clear() {
    boxes.getChildren().clear();
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
            Box box = new Box(boxSize, boxSize, boxSize);
            box.translateXProperty().setValue(boxSize * (x + offset.getX() - (sizeX / 2)));
            box.translateYProperty().setValue(boxSize * (y + offset.getY() - (sizeY / 2)));
            box.translateZProperty().setValue(boxSize * (z + offset.getZ() - (sizeZ / 2)));
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
