package org.example;

import org.joml.Vector2i;

import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OverlappingModel {

    private Grid input;
    private int patternSize;
    private int maximumTries = 10000000;
    private int maxPropagationTries = 100;

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

    public void solve() {
        initializeWave();
        baseEntropy = getEntropy(0);
        int collapsedCells = 0;
        int tries = 0;
        int propagationTries = 0;
        this.entropy = wave.stream().map(this::getEntropy).collect(Collectors.toList());
        while (collapsedCells < outputSize.x * outputSize.y && tries < maximumTries) {
            //solve
            //collapse min entropy cell
            int minEntropyIndex = getLowestEntropyCell();
            wave.set(minEntropyIndex, Collections.singletonList(selectRandomPattern(minEntropyIndex)));
            this.entropy.set(minEntropyIndex, getEntropy(minEntropyIndex));

            //create backup if propagation fails
            List<List<Integer>> snapshot = new ArrayList<>();
            wave.forEach(cell -> snapshot.add(new ArrayList<>(cell)));

            boolean success = propagate(minEntropyIndex);

            if(!success) {
                if(propagationTries >= maxPropagationTries) {
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
        if(tries >= maximumTries) {
            System.out.println("No solution after " + tries + " tries.");
        } else {
            System.out.println("Success after " + tries + " tries.");
        }
        System.out.println(Utils.print2DArray(generateOutput()));
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
        int[][] grid = new int[outputSize.y][outputSize.x];
        for (int y = 0; y < outputSize.y; y++) {
            for (int x = 0; x < outputSize.x; x++) {
                Pattern pattern = patterns.get(getWaveAt(x, y).get(0));
//                grid[y][x] = getWaveAt(x, y).get(0);
                grid[y][x] = pattern.get(0, 0);
//                Pattern pattern = patterns.get(getWaveAt(x, y).get(0));
//                int nx = x * 2;
//                int ny = y * 2;
//                grid.set(nx, ny, pattern.get(0, 0));
//                grid.set(nx + 1, ny, pattern.get(1, 0));
//                grid.set(nx, ny + 1, pattern.get(0, 1));
//                grid.set(nx + 1, ny + 1, pattern.get(1, 1));
            }
        }
        return grid;
    }

    private boolean propagate(int cellIndex) {
        Queue<Integer> cellsToPropagate = new LinkedList<>();

        cellsToPropagate.add(cellIndex);
        while(!cellsToPropagate.isEmpty()) {
            int currentCell = cellsToPropagate.poll();
            Integer pattern = wave.get(currentCell).get(0);

            Vector2i cellPosition = new Vector2i(currentCell % outputSize.x, currentCell / outputSize.y);

            for (int i = 0; i < Direction.values().length; i++) {
                Direction direction = Direction.values()[i];
                Vector2i dir = Utils.DIRECTIONS.get(i);

                Vector2i neighbourPosition = new Vector2i(cellPosition).add(dir);
                if (neighbourPosition.x < 0 || neighbourPosition.x >= outputSize.x || neighbourPosition.y < 0 || neighbourPosition.y >= outputSize.y) {
                    continue;
                }
                List<Integer> neighbourCell = getWaveAt(neighbourPosition.x, neighbourPosition.y);

                if (neighbourCell.size() == 1) continue;

                List<Integer> possiblePatterns = neighbourCell.stream()
                        .filter(neighbourPattern -> patternNeighbours[pattern].neighbours.get(direction).contains(neighbourPattern))
                        .collect(Collectors.toList());

                int neighbourIndex = neighbourPosition.x + outputSize.x * neighbourPosition.y;

                wave.set(neighbourIndex, possiblePatterns);
                this.entropy.set(neighbourIndex, getEntropy(neighbourIndex));

                if (possiblePatterns.size() == 0) {/* restart algorithm */
//                throw new RuntimeException("no solution");
                    return false;
                }

                if (possiblePatterns.size() == 1) {/* cell is decided */
                    cellsToPropagate.add(neighbourIndex);
                }
            }
        }
        return true;
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
        for (int x = 0; x < input.size().x(); x++) {
            for (int y = 0; y < input.size().y(); y++) {
                Pattern pattern = input.getPatternAtPosition(new Vector2i(x, y), patternSize);
                if (!patterns.contains(pattern)) {
                    patterns.add(pattern);
                    patternFrequency.add(1d);
                } else {
                    int patternIndex = patterns.indexOf(pattern);
                    Double patternFrequency = this.patternFrequency.get(patternIndex);
                    this.patternFrequency.set(patternIndex, patternFrequency + 1);
                }
            }
        }

        int totalPatternCount = input.size().x * input.size().y;
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

    private int getLowestEntropyCell() {
        double min = Double.MAX_VALUE;
        int minIndex = -1;
        for (int i = 0; i < entropy.size(); i++) {
            if (entropy.get(i) > 0 && entropy.get(i) < min) {
                min = entropy.get(i);
                minIndex = i;
            }
        }
        if(minIndex == -1) {
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
        for (int i = 0; i < cell.size(); i++) {
            Double nextFrequency = frequencies.get(i);
            if (acc >= rand && rand <= (acc + nextFrequency)) {
                return i;
            }
            acc += nextFrequency;
        }
        return 0;
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
