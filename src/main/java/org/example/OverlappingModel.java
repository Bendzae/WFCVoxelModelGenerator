package org.example;

import org.joml.Vector2i;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OverlappingModel {

    private Grid input;
    private int patternSize;
    private boolean rotation = true;
    private boolean symmetry = true;
    private int maximumTries = 100000;
    private int maxPropagationTries = 0;

    public List<Pattern> patterns;

    public HashMap<Integer, Integer> colorFrequency;
    public List<Double> patternFrequency;
    public Neighbours[] patternNeighbours;

    public List<List<Integer>> wave;
    public List<Double> entropy;
    public Vector2i outputSize;

    private Double baseEntropy;

    public OverlappingModel(int[][] input, int patternSize, Vector2i outputSize) {
        this.input = new Grid(input);
        this.patternSize = patternSize;
        this.outputSize = outputSize;

        computeColorFrequency();

        findPatterns();

        findNeighbours();
    }

    public int[][] solve() {
        initializeWave();
        baseEntropy = getEntropy(0);
        int collapsedCells = 0;
        int tries = 0;
        int propagationTries = 0;
        this.entropy = wave.stream().map(this::getEntropy).collect(Collectors.toList());
        while (collapsedCells < outputSize.x * outputSize.y && tries < maximumTries) {
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
        } else {
            System.out.println("Success after " + tries + " tries.");
        }

        //***Test****
        int errors1 = checkForErrors();

        //*********
        int[][] array = generateOutput();
        System.out.println(Utils.print2DArray(array));
        return array;
    }

    private boolean propagate(int cellIndex) {
        Queue<Integer> cellsToPropagate = new LinkedList<>();

        cellsToPropagate.add(cellIndex);
        while (!cellsToPropagate.isEmpty()) {
            int currentCell = cellsToPropagate.poll();

            Vector2i cellPosition = new Vector2i(currentCell % outputSize.x, currentCell / outputSize.y);

            for (int i = 0; i < Direction.values().length; i++) {
                Direction direction = Direction.values()[i];
                Vector2i dir = Utils.DIRECTIONS.get(i);

                Vector2i neighbourPosition = new Vector2i(cellPosition).add(dir);
                if (neighbourPosition.x < 0 || neighbourPosition.x >= outputSize.x || neighbourPosition.y < 0 || neighbourPosition.y >= outputSize.y) {
                    continue;
                }
                List<Integer> neighbourCell = getWaveAt(neighbourPosition.x, neighbourPosition.y);

                HashSet<Integer> possiblePatterns = new HashSet<>();
                List<Integer> currentPatterns = wave.get(currentCell);
                for (Integer pattern : currentPatterns) {
                    List<Integer> patterns = neighbourCell.stream()
                            .filter(neighbourPattern -> patternNeighbours[pattern].neighbours.get(direction).contains(neighbourPattern))
                            .collect(Collectors.toList());
                    possiblePatterns.addAll(patterns);
                }

                int neighbourIndex = neighbourPosition.x + outputSize.x * neighbourPosition.y;
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

        for (int x = 0; x < outputSize.x(); x++) {
            for (int y = 0; y < outputSize.y(); y++) {
                wave.add(new ArrayList<>());
                List<Integer> waveCell = wave.get(wave.size() - 1);
                for (int i = 0; i < patterns.size(); i++) {
                    waveCell.add(i);
                }
            }
        }
    }

    private int[][] generateOutput() {
        int sizeFactor = patternSize - 1;
        int sizeX = outputSize.x * sizeFactor + 1;
        int sizeY = outputSize.y * sizeFactor + 1;
        int[][] grid = new int[sizeX][sizeY];
        for (int y = 0; y < outputSize.y; y++) {
            for (int x = 0; x < outputSize.x; x++) {
                Pattern pattern = patterns.get(getWaveAt(x, y).get(0));
                for (int px = 0; px < patternSize; px++) {
                    for (int py = 0; py < patternSize; py++) {
                        grid[y * sizeFactor + py][x * sizeFactor + px] = pattern.get(px,py);
                    }
                }
            }
        }
        return grid;
    }

    private double getEntropy(int cellIndex) {
        return getEntropy(wave.get(cellIndex));
    }

    private double getEntropy(Vector2i cellPosition) {
        return getEntropy(getWaveAt(cellPosition.x, cellPosition.y));
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

    private List<Integer> getWaveAt(int x, int y) {
        return wave.get(x + y * outputSize.x());
    }

    public void computeColorFrequency() {
        this.colorFrequency = new HashMap<>();
        for (int x = 0; x < input.size().x(); x++) {
            for (int y = 0; y < input.size().y(); y++) {
                int color = input.get(x, y);
                if (!colorFrequency.containsKey(color)) {
                    colorFrequency.put(color, 1);
                } else {
                    Integer colorCount = colorFrequency.get(color);
                    colorFrequency.put(color, colorCount + 1);
                }
            }
        }
    }

    public void findPatterns() {
        this.patterns = new ArrayList<>();
        this.patternFrequency = new ArrayList<>();
        int boundX = input.size().x();
        int boundY = input.size().y();
        for (int x = 0; x < boundX; x++) {
            for (int y = 0; y < boundY; y++) {
                ArrayList<Pattern> patterns = new ArrayList<>();
                Pattern currentPattern = input.getPatternAtPosition(new Vector2i(x, y), patternSize);
                patterns.add(currentPattern);
                if (rotation) {
                    patterns.addAll(input.getRotatedPatterns(currentPattern));
                }
                if (symmetry) {
                    patterns.addAll(input.getReflectedPatterns(currentPattern));
                }
                for (Pattern pattern : patterns) {
                    if (!this.patterns.contains(pattern)) {
                        this.patterns.add(pattern);
                        patternFrequency.add(1d);
                    } else {
                        int patternIndex = this.patterns.indexOf(pattern);
                        Double patternFrequency = this.patternFrequency.get(patternIndex);
                        this.patternFrequency.set(patternIndex, patternFrequency + 1);
                    }
                }
            }
        }

        int totalPatternCount = boundX * boundY;
        for (int i = 0; i < patternFrequency.size(); i++) {
            double repetitions = patternFrequency.get(i);
            patternFrequency.set(i, repetitions / totalPatternCount);
        }
    }

    private void findNeighbours() {
        this.patternNeighbours = new Neighbours[patterns.size()];

        for (int index = 0; index < patterns.size(); index++) {
            Pattern currentPattern = patterns.get(index);
            for (int otherIndex = 0; otherIndex < patterns.size(); otherIndex++) {
                Pattern otherPattern = patterns.get(otherIndex);
                if (patternNeighbours[index] == null) patternNeighbours[index] = new Neighbours();

                if (Arrays.equals(currentPattern.getEdge(Direction.LEFT), otherPattern.getEdge(Direction.RIGHT))) {
                    patternNeighbours[index].addNeighbour(Direction.LEFT, otherIndex);
                }
                if (Arrays.equals(currentPattern.getEdge(Direction.RIGHT), otherPattern.getEdge(Direction.LEFT))) {
                    patternNeighbours[index].addNeighbour(Direction.RIGHT, otherIndex);
                }
                if (Arrays.equals(currentPattern.getEdge(Direction.UP), otherPattern.getEdge(Direction.DOWN))) {
                    patternNeighbours[index].addNeighbour(Direction.UP, otherIndex);
                }
                if (Arrays.equals(currentPattern.getEdge(Direction.DOWN), otherPattern.getEdge(Direction.UP))) {
                    patternNeighbours[index].addNeighbour(Direction.DOWN, otherIndex);
                }
            }
        }
    }

    private void findNeighboursStrict() {

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
            throw new RuntimeException("couldnt find lowest entropy cell.");
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

    private int checkForErrors() {
        AtomicInteger errors = new AtomicInteger();
        for (int x = 0; x < outputSize.x; x++) {
            for (int y = 0; y < outputSize.y; y++) {
                for (int i = 0; i < 4; i++) {
                    Direction dir = Direction.values()[i];
                    Vector2i dirV = Utils.DIRECTIONS.get(i);

                    HashSet<Integer> possiblePatternsTest = new HashSet<>();
                    List<Integer> current = getWaveAt(x, y);
                    for (Integer pattern : current) {
                        possiblePatternsTest.addAll(patternNeighbours[pattern].neighbours.get(dir));
                    }

                    final int currentIndex = x + y * outputSize.x;

                    int xn = x + dirV.x;
                    int yn = y + dirV.y;

                    if (xn < 0 || xn >= outputSize.x || yn < 0 || yn >= outputSize.y) continue;

                    List<Integer> neighbour = getWaveAt(xn, yn);

                    neighbour.forEach(p -> {
                        for (Integer pattern : current) {
                            if (!possiblePatternsTest.contains(p)) {
//                                System.out.println("direction: " + dir + " pattern: ");
//                                System.out.println(patterns.get(pattern));
//                                System.out.println("with pattern: ");
//                                System.out.println(patterns.get(p));
                                System.out.println("pattern " + p + " in cell " + (xn + yn * outputSize.x) + " cant be " + dir + " of cell " + currentIndex);
                                errors.getAndIncrement();
                            }
                        }
                    });
                }
            }
        }
        if (errors.get() > 0) System.out.println("neighbour constraints not satisfied: " + errors + " errors.");
        return errors.get();
    }

    class Neighbours {
        public HashMap<Direction, HashSet<Integer>> neighbours;

        public Neighbours() {
            this.neighbours = new HashMap<>();
            Stream.of(Direction.values()).forEach(d -> neighbours.put(d, new HashSet<>()));
        }

        public boolean addNeighbour(Direction direction, Integer neighbour) {
            return neighbours.get(direction).add(neighbour);
        }
    }

}
