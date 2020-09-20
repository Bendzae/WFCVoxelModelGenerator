package org.example.wfc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;

class Neighbours {

  public HashMap<Direction3D, HashSet<Integer>> neighbours;

  public Neighbours() {
    this.neighbours = new HashMap<>();
    Stream.of(Direction3D.values()).forEach(d -> neighbours.put(d, new HashSet<>()));
  }

  public boolean addNeighbour(Direction3D direction, Integer neighbour) {
    return neighbours.get(direction).add(neighbour);
  }
}
