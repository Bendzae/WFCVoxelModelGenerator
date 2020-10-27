package org.example.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.example.shared.Vector3;
import org.example.voxparser.VoxFile;
import org.example.voxparser.VoxModel;
import org.example.voxparser.VoxReader;
import org.example.voxparser.VoxSerializer;
import org.example.voxparser.Voxel;

public class ModelImporterExporter {

  public static VoxelViewModel loadVoxelModelFromFile(String filepath) {
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

    VoxModel[] models = voxFile.getModels();
    VoxModel model = models[0];

    return new VoxelViewModel(VoxModeltoArray(model), voxFile.getPalette(), model.getSize());
  }

  public static void writeVoxelModelToFile(int[][][] model, int[] palette, File file) {
    VoxSerializer voxSerializer = new VoxSerializer();
    voxSerializer.writeToVox(ModelImporterExporter.arrayToVoxModel(model), palette, file);
  }

  private static VoxModel arrayToVoxModel(int[][][] model) {
    //Create from 3D-Array
    List<Voxel> voxels = new ArrayList<>();
    int sizeX = model[0][0].length;
    int sizeZ = model[0].length;
    int sizeY = model.length;
    for (int x = 0; x < sizeX; x++) {
      for (int y = 0; y < sizeY; y++) {
        for (int z = 0; z < sizeZ; z++) {
          int colorIndex = model[y][sizeZ - z - 1][x];
          if (colorIndex >= 0) {
            voxels.add(new Voxel(new Vector3<Byte>((byte) x, (byte) y, (byte) z), (byte) colorIndex));
          }
        }
      }
    }
    return new VoxModel(new Vector3<>(sizeX, sizeY, sizeZ), voxels.toArray(Voxel[]::new));
  }

  private static int[][][] VoxModeltoArray(VoxModel voxModel) {
    Vector3<Integer> size = voxModel.getSize();
    int[][][] out = new int[size.getY()][size.getZ()][size.getX()];

    for (int x = 0; x < size.getX(); x++) {
      for (int y = 0; y < size.getY(); y++) {
        for (int z = 0; z < size.getZ(); z++) {
          out[y][z][x] = -1;
        }
      }

    }

    Arrays.stream(voxModel.getVoxels()).forEach(voxel -> {
      Vector3<Byte> position = voxel.getPosition();
      out[position.getY()][size.getZ() - position.getZ() - 1][position.getX()] = voxel
          .getColourIndex(); //Accounting for different coordinate systems
    });
    return out;
  }
}
