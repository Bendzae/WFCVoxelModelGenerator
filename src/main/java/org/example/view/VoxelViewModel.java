package org.example.view;

import org.example.voxparser.Vector3;

public class VoxelViewModel {

  int[][][] voxelData;
  int[] palette;
  Vector3<Integer> size;

  public VoxelViewModel(int[][][] voxelData, int[] palette, Vector3<Integer> size) {
    this.voxelData = voxelData;
    this.palette = palette;
    this.size = size;
  }

  public int[][][] getVoxelData() {
    return voxelData;
  }

  public int[] getPalette() {
    return palette;
  }

  public Vector3<Integer> getSize() {
    return size;
  }
}
