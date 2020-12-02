package org.example.shared;

/**
 * Parameters of the Voxel-WFC for serialization.
 */
public class VoxelWFCParameters {

  public int patternSize;
  public int outputSizeX;
  public int outputSizeY;
  public int outputSizeZ;
  public boolean rotation;
  public double avoidEmptyPattern;

  public VoxelWFCParameters(
      int patternSize,
      int outputSizeX,
      int outputSizeY,
      int outputSizeZ,
      boolean rotation,
      double avoidEmptyPattern
  ) {
    this.patternSize = patternSize;
    this.outputSizeX = outputSizeX;
    this.outputSizeY = outputSizeY;
    this.outputSizeZ = outputSizeZ;
    this.rotation = rotation;
    this.avoidEmptyPattern = avoidEmptyPattern;
  }
}
