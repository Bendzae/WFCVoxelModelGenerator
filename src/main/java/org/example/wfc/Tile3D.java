package org.example.wfc;


public class Tile3D {

  private int tilesize;
  private int[][][] voxels;
  private Neighbours neighbours;
  private int frequency;

  public Tile3D(int tilesize, int[][][] voxels) {
    this.tilesize = tilesize;
    this.voxels = voxels;
    this.neighbours = new Neighbours();
    this.frequency = 1;
  }

  public int[][][] getVoxels() {
    return voxels;
  }

  public Neighbours getNeighbours() {
    return neighbours;
  }

  public boolean addNeighbour(Direction3D direction, Integer neigbour) {
    return this.neighbours.addNeighbour(direction, neigbour);
  }

  public int getTileSize() {
    return this.tilesize;
  }

  public void setFrequency(int frequency) {
    this.frequency = frequency;
  }

  public int getFrequency() {
    return frequency;
  }
}
