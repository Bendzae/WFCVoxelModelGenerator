package org.example.wfc;

import org.example.voxparser.Vector3;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleModel3D {

    private Grid3D input;
    private int patternSize;
    private boolean rotation;
    private boolean symmetry;
    private int maximumTries = 5000;
    private int maxPropagationTries = 10;

    public List<Pattern3D> patterns;
    public HashMap<Vector3<Integer>, Integer> patternsByPosition;
    private int uniquePatternCount;

    public List<Double> patternFrequency;
    public Neighbours[] patternNeighbours;

    public List<List<Integer>> wave;
    public List<Double> entropy;
    public Vector3<Integer> outputSize;

    private Double baseEntropy;

    public SimpleModel3D(int[][][] input, int patternSize, Vector3<Integer> outputSize, boolean rotation, boolean symmetry) {
        this.input = new Grid3D(input);
        this.patternSize = patternSize;
        this.rotation = rotation;
        this.symmetry = symmetry;
//        int calculatedOutputSize = outputSize.x / (patternSize - 1);
//        this.outputSize = new Vector2i(calculatedOutputSize, calculatedOutputSize);
        this.outputSize = outputSize;

        //TODO Remove later
//        if (this.input.size().getX() % patternSize != 0
//                || this.input.size().getY() % patternSize != 0
//                || this.input.size().getZ() % patternSize != 0) {
//            throw new RuntimeException("input size should be divisible by pattern size (for now)");
//        }

        findPatterns();
        findNeighbours();
    }

    public int[][][] solve() {
        initializeWave();
        baseEntropy = getEntropy(0);
        int collapsedCells = 0;
        int tries = 0;
        int propagationTries = 0;
        this.entropy = wave.stream().map(this::getEntropy).collect(Collectors.toList());
        while (collapsedCells < outputSize.getX() * outputSize.getY() * outputSize.getZ() && tries < maximumTries) {
            //create backup if propagation fails
            List<List<Integer>> snapshot = new ArrayList<>();
            wave.forEach(cell -> snapshot.add(new ArrayList<>(cell)));

            //solve
            //collapse min entropy cell
            int minEntropyIndex = getLowestEntropyCell();
            wave.set(minEntropyIndex, Collections.singletonList(selectRandomPattern(minEntropyIndex)));
            this.entropy.set(minEntropyIndex, getEntropy(minEntropyIndex));


            boolean success = propagate(minEntropyIndex);
//            if (checkForErrors() > 0) {
////                //throw new RuntimeException("Error after prop");
////            }

            if (!success) {
                if (propagationTries >= maxPropagationTries) {
                    tries++;
                    initializeWave();
                    this.entropy = wave.stream().map(this::getEntropy).collect(Collectors.toList());
                    collapsedCells = 0;
                } else {
                    wave = snapshot;
                    this.entropy = wave.stream().map(this::getEntropy).collect(Collectors.toList());
                    propagationTries++;
                }
            } else {
                propagationTries = 0;
            }
            collapsedCells = (int) wave.stream().filter(l -> l.size() == 1).count();
        }
        System.out.println();
        if (tries >= maximumTries) {
            System.out.println("No solution after " + tries + " tries.");
            return null;
        } else {
            System.out.println("Success after " + tries + " tries.");
        }

        //***Test****
//        int errors1 = checkForErrors();

        //*********
        int[][][] array = generateOutput();
        return array;
    }

    private boolean propagate(int cellIndex) {
        Queue<Integer> cellsToPropagate = new LinkedList<>();

        cellsToPropagate.add(cellIndex);
        while (!cellsToPropagate.isEmpty()) {
            int currentCell = cellsToPropagate.poll();

            Vector3<Integer> cellPosition = getPosFromCellIndex(cellIndex);

            for (int i = 0; i < Direction3D.values().length; i++) {
                Direction3D direction = Direction3D.values()[i];
                Vector3<Integer> dir = Utils.DIRECTIONS3D.get(i);

                Vector3<Integer> neighbourPosition = new Vector3<Integer>(
                        cellPosition.getX() + dir.getX(),
                        cellPosition.getY() + dir.getY(),
                        cellPosition.getZ() + dir.getZ()
                );

                if (neighbourPosition.getX() < 0
                        || neighbourPosition.getX() >= outputSize.getX()
                        || neighbourPosition.getY() < 0
                        || neighbourPosition.getY() >= outputSize.getY()
                        || neighbourPosition.getZ() < 0
                        || neighbourPosition.getZ() >= outputSize.getZ()
                ) {
                    continue;
                }
                List<Integer> neighbourCell = getWaveAt(neighbourPosition.getX(), neighbourPosition.getY(), neighbourPosition.getZ());

                HashSet<Integer> possiblePatterns = new HashSet<>();
                List<Integer> currentPatterns = wave.get(currentCell);
                for (Integer pattern : currentPatterns) {
                    List<Integer> patterns = neighbourCell.stream()
                            .filter(neighbourPattern -> patternNeighbours[pattern].neighbours.get(direction).contains(neighbourPattern))
                            .collect(Collectors.toList());
                    possiblePatterns.addAll(patterns);
                }

                int neighbourIndex = getCellIndexFromPos(neighbourPosition.getX(), neighbourPosition.getY(), neighbourPosition.getZ());
                List<Integer> newPatterns = new ArrayList<Integer>(possiblePatterns);
                if (newPatterns.size() < neighbourCell.size()) {
                    wave.set(neighbourIndex, newPatterns);
                    this.entropy.set(neighbourIndex, getEntropy(neighbourIndex));
                    cellsToPropagate.add(neighbourIndex);
                }

                if (possiblePatterns.size() == 0) {/* restart algorithm */
                    return false;
                }
            }
        }
        return true;
    }

    private void initializeWave() {
        wave = new ArrayList<>();

        for (int x = 0; x < outputSize.getX(); x++) {
            for (int y = 0; y < outputSize.getY(); y++) {
                for (int z = 0; z < outputSize.getZ(); z++) {
                    wave.add(new ArrayList<>());
                    List<Integer> waveCell = wave.get(wave.size() - 1);
                    for (int i = 0; i < patterns.size(); i++) {
                        waveCell.add(i);
                    }
                }
            }
        }
    }

    private int[][][] generateOutput() {
        int sizeX = outputSize.getX() * patternSize;
        int sizeY = outputSize.getY() * patternSize;
        int sizeZ = outputSize.getZ() * patternSize;
        int[][][] grid = new int[sizeZ][sizeY][sizeX];
        for (int z = 0; z < outputSize.getZ(); z++) {
            for (int y = 0; y < outputSize.getY(); y++) {
                for (int x = 0; x < outputSize.getX(); x++) {
                    Pattern3D pattern = patterns.get(getWaveAt(x, y, z).get(0));
                    for (int px = 0; px < patternSize; px++) {
                        for (int py = 0; py < patternSize; py++) {
                            for (int pz = 0; pz < patternSize; pz++) {
                                grid[z * patternSize + pz][y * patternSize + py][x * patternSize + px] = pattern.get(px, py, pz);
                            }
                        }
                    }
                }
            }
        }
        return grid;
    }

    private double getEntropy(int cellIndex) {
        return getEntropy(wave.get(cellIndex));
    }

    private double getEntropy(Vector3<Integer> cellPosition) {
        return getEntropy(getWaveAt(cellPosition.getX(), cellPosition.getY(), cellPosition.getZ()));
    }

    private double getEntropy(List<Integer> cell) {

        if (cell.size() == 1) return 0;

        double sumOfWeights = cell.stream()
                .map(index -> patternFrequency.get(index))
                .reduce(0d, Double::sum, Double::sum);

        double logSumOfWeights = cell.stream()
                .map(index -> patternFrequency.get(index))
                .reduce(0d, (sum, weight) -> sum + (weight * log2(weight)), Double::sum);

        return log2(sumOfWeights) - (logSumOfWeights / sumOfWeights) + (2e-10 * Math.random());
    }

    private double log2(double value) {
        return Math.log(value) / Math.log(2);
    }

    public int getCellIndexFromPos(int x, int y, int z) {
        return x + y * outputSize.getX() + z * outputSize.getX() * outputSize.getY();
    }

    private List<Integer> getWaveAt(int x, int y, int z) {
        return wave.get(getCellIndexFromPos(x, y, z));
    }

    public Vector3<Integer> getPosFromCellIndex(int index) {
        int x = index % outputSize.getX();
        int y = (index / outputSize.getX()) % outputSize.getY();
        int z = index / (outputSize.getX() * outputSize.getY());
        return new Vector3<>(x, y, z);
    }

    public void findPatterns() {
        Pattern3D emptyPattern = new Pattern3D(patternSize);
        this.patterns = new ArrayList<>();
        //Add empty pattern at pos 0
        this.patterns.add(emptyPattern);
        this.patternsByPosition = new HashMap<>();
        this.patternFrequency = new ArrayList<>();
        this.patternFrequency.add(0.000001);
        int boundX = input.size().getX();
        int boundY = input.size().getY();
        int boundZ = input.size().getZ();
        for (int x = 0; x <= boundX; x += patternSize) {
            for (int y = 0; y <= boundY; y += patternSize) {
                for (int z = 0; z <= boundZ; z += patternSize) {
                    ArrayList<Pattern3D> tempPatterns = new ArrayList<>();
                    Pattern3D currentPattern = input.getPatternAtPosition(new Vector3(x, y, z), patternSize);
                    if (currentPattern != null) {
                        tempPatterns.add(currentPattern);
//                    if (rotation) {
//                        tempPatterns.addAll(input.getRotatedPatterns(currentPattern));
//                    }
//                    if (symmetry) {
//                        tempPatterns.addAll(input.getReflectedPatterns(currentPattern));
//                    }
                        Vector3<Integer> patternPosition = new Vector3<>(x / patternSize, y / patternSize, z / patternSize);
                        for (Pattern3D pattern : tempPatterns) {
                            if (!this.patterns.contains(pattern)) {
                                this.patterns.add(pattern);
                                this.patternsByPosition.put(patternPosition, patterns.size() - 1);
                                this.patternFrequency.add(1d);
                            } else {
                                int patternIndex = this.patterns.indexOf(pattern);
                                this.patternsByPosition.put(patternPosition, patternIndex);
                                if (patternIndex != 0) { //dont count empty pattern to discourage empty solution TODO remove later
                                    Double patternFrequency = this.patternFrequency.get(patternIndex);
                                    this.patternFrequency.set(patternIndex, patternFrequency + 1);
                                }
                            }
                        }
                    }
                }
            }
        }
        this.uniquePatternCount = this.patterns.size();
        int totalPatternCount = (boundX / patternSize) * (boundY / patternSize) * (boundZ / patternSize);
        for (int i = 0; i < patternFrequency.size(); i++) {
            double repetitions = patternFrequency.get(i);
            patternFrequency.set(i, repetitions / totalPatternCount);
        }

        if(rotation) {
            List<Pattern3D> yRotated = this.patterns.stream().map(p -> p.getYRotated()).collect(Collectors.toList());
            this.patterns.addAll(yRotated);
            this.patternFrequency.addAll(Collections.unmodifiableList(this.patternFrequency));
        }
    }

    private void findNeighbours() {

        this.patternNeighbours = new Neighbours[this.patterns.size()];
        for (int i = 0; i < patterns.size(); i++) {
            this.patternNeighbours[i] = new Neighbours();
        }

        for (int i = 0; i < 6; i++) {
            Direction3D dir = Direction3D.values()[i];
            for (int j = 0; j < patterns.size(); j++) {
                this.patternNeighbours[0].addNeighbour(dir, j);
            }
        }

        patternsByPosition.forEach((position, patternIndex) -> {
            for (int i = 0; i < 6; i++) {
                Direction3D dir = Direction3D.values()[i];
                Vector3<Integer> dirV = Utils.DIRECTIONS3D.get(i);

                Vector3<Integer> newPos =
                        new Vector3<>(position.getX() + dirV.getX(), position.getY() + dirV.getY(), position.getZ() + dirV.getZ());

                if (patternsByPosition.containsKey(newPos)) {
                    this.patternNeighbours[patternIndex].addNeighbour(dir, patternsByPosition.get(newPos));
                } else {
                    this.patternNeighbours[patternIndex].addNeighbour(dir, 0);
                }
            }
        });

        if(rotation) {
            for (int i = 0; i < this.uniquePatternCount; i++) {
                Neighbours originalNeighbours = this.patternNeighbours[i];

                int finalI = i;
                Arrays.stream(Direction3D.values()).forEach(direction -> {
                    HashSet<Integer> originalPatterns = originalNeighbours.neighbours.get(direction);
                    HashSet<Integer> rotatedPatterns = (HashSet<Integer>) originalPatterns.stream()
                        .map(patternIndex -> patternIndex + this.uniquePatternCount).collect(Collectors.toSet());
                    this.patternNeighbours[finalI + uniquePatternCount].neighbours.put(Utils.rotateYDir(direction), rotatedPatterns);
                } );
            }
        }
    }

    private int getLowestEntropyCell() {
        double min = Double.MAX_VALUE;
        int minIndex = -1;
        for (int i = 0; i < entropy.size(); i++) {
            if (entropy.get(i) > 0 && entropy.get(i) < min) {
                min = entropy.get(i);
                minIndex = i;
            }
        }
        if (minIndex == -1) {
//            throw new RuntimeException("couldnt find lowest entropy cell.");
            return 0;
        }
        return minIndex;
    }

    private int selectRandomPattern(int cellIndex) {
        List<Integer> cell = wave.get(cellIndex);

        List<Double> frequencies = cell.stream().map(pattern -> patternFrequency.get(pattern)).collect(Collectors.toList());
        double total = frequencies.stream().reduce(0d, Double::sum);

        double rand = Math.random() * total;
        double acc = 0;
        for (int i = 0; i < cell.size() - 1; i++) {
            int pattern = cell.get(i);
            acc += frequencies.get(i);
            if (acc >= rand && rand <= (acc + frequencies.get(i + 1))) {
                return pattern;
            }
        }
        return cell.get(cell.size() - 1);
    }

//    private int checkForErrors() {
//        AtomicInteger errors = new AtomicInteger();
//        for (int x = 0; x < outputSize.getX(); x++) {
//            for (int y = 0; y < outputSize.getY(); y++) {
//                for (int i = 0; i < 4; i++) {
//                    Direction dir = Direction.values()[i];
//                    Vector2i dirV = Utils.DIRECTIONS.get(i);
//
//                    HashSet<Integer> possiblePatternsTest = new HashSet<>();
//                    List<Integer> current = getWaveAt(x, y, z);
//                    for (Integer pattern : current) {
//                        possiblePatternsTest.addAll(patternNeighbours[pattern].neighbours.get(dir));
//                    }
//
//                    final int currentIndex = x + y * outputSize.getX();
//
//                    int xn = x + dirV.x;
//                    int yn = y + dirV.y;
//
//                    if (xn < 0 || xn >= outputSize.getX() || yn < 0 || yn >= outputSize.getY()) continue;
//
//                    List<Integer> neighbour = getWaveAt(xn, yn);
//
//                    neighbour.forEach(p -> {
//                        for (Integer pattern : current) {
//                            if (!possiblePatternsTest.contains(p)) {
////                                System.out.println("direction: " + dir + " pattern: ");
////                                System.out.println(patterns.get(pattern));
////                                System.out.println("with pattern: ");
////                                System.out.println(patterns.get(p));
//                                System.out.println("pattern " + p + " in cell " + (xn + yn * outputSize.getX()) + " cant be " + dir + " of cell " + currentIndex);
//                                errors.getAndIncrement();
//                            }
//                        }
//                    });
//                }
//            }
//        }
//        if (errors.get() > 0) System.out.println("neighbour constraints not satisfied: " + errors + " errors.");
//        return errors.get();
//    }

    private class Neighbours {
        public HashMap<Direction3D, HashSet<Integer>> neighbours;

        public Neighbours() {
            this.neighbours = new HashMap<>();
            Stream.of(Direction3D.values()).forEach(d -> neighbours.put(d, new HashSet<>()));
        }

        public boolean addNeighbour(Direction3D direction, Integer neighbour) {
            return neighbours.get(direction).add(neighbour);
        }
    }
}
