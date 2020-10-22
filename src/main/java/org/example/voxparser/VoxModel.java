package org.example.voxparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class VoxModel {

  private final Vector3<Integer> size;
  private final Voxel[] voxels;

  public VoxModel(Vector3<Integer> size, Voxel[] voxels) {
    if (size == null || voxels == null) {
      throw new IllegalArgumentException("Both size and voxels must be non-null");
    }

    this.size = size;
    this.voxels = voxels;
  }

  //Create from 3D-Array
  public VoxModel(int[][][] model) {
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

    this.size = new Vector3<>(sizeX, sizeY, sizeZ);
    this.voxels = voxels.toArray(Voxel[]::new);
  }

  public Vector3<Integer> getSize() {
    return size;
  }

  public Voxel[] getVoxels() {
    return voxels;
  }

  public int[][][] to3DArray() {
    int[][][] out = new int[size.getY()][size.getZ()][size.getX()];

    for (int x = 0; x < size.getX(); x++) {
      for (int y = 0; y < size.getY(); y++) {
        for (int z = 0; z < size.getZ(); z++) {
          out[y][z][x] = -1;
        }
      }

    }

    Arrays.stream(voxels).forEach(voxel -> {
      Vector3<Byte> position = voxel.getPosition();
      out[position.getY()][size.getZ() - position.getZ() - 1][position.getX()] = voxel
          .getColourIndex(); //Accounting for different coordinate systems
    });
    return out;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VoxModel voxModel = (VoxModel) o;
    return size.equals(voxModel.size) &&
        Arrays.equals(voxels, voxModel.voxels);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(size);
    result = 31 * result + Arrays.hashCode(voxels);
    return result;
  }
}
