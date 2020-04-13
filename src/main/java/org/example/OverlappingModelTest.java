package org.example;


import org.joml.Vector2i;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class OverlappingModelTest {
    private int[][] input = new int[][]{
            {1, 2, 3},
            {0, 1, 0},
            {0, 2, 0},
    };
    private int[][] input2 = new int[][]{
            {0, 0, 0, 0},
            {1, 1, 0, 1},
            {0, 0, 0, 0},
            {0, 0, 0, 0},
    };
    private int[][] input3 = new int[][]{
            {0, 0, 0, 0, 0},
            {0, 2, 2, 2, 0},
            {0, 2, 1, 2, 0},
            {0 ,2, 1, 2, 0},
            {0 ,2, 2, 2, 0},
    };

    OverlappingModel overlappingModel1;
    OverlappingModel overlappingModel2;
    OverlappingModel overlappingModel3;

    @Before
    public void setUp() throws Exception {
        overlappingModel1 = new OverlappingModel(input, 2, new Vector2i(10, 10));
        overlappingModel2 = new OverlappingModel(input2, 2, new Vector2i(10, 10));
        overlappingModel3 = new OverlappingModel(input3, 2, new Vector2i(20,20));
    }

    @Test
    public void colorFrequencyTest() {
        OverlappingModel overlappingModel = overlappingModel1;
        HashMap<Integer, Integer> colorFrequency = overlappingModel.colorFrequency;
        assertEquals(4, (long) colorFrequency.get(0));
        assertEquals(2, (long) colorFrequency.get(1));
        assertEquals(2, (long) colorFrequency.get(2));
        assertEquals(1, (long) colorFrequency.get(3));
    }

    @Test
    public void getValueWrapped() {
        Grid grid = new Grid(input);
        System.out.println(grid);
        int v1 = grid.getWrapped(0, 0);
        int v2 = grid.getWrapped(2, 2);
        int v3 = grid.getWrapped(3, 0);
        int v4 = grid.getWrapped(1, 5);
        assertEquals(1, (long) v1);
        assertEquals(0, (long) v2);
        assertEquals(1, (long) v3);
        assertEquals(2, (long) v4);
    }

    @Test
    public void getPatternAtPosition() {
        Grid grid = new Grid(input);
        Pattern pattern1 = grid.getPatternAtPosition(new Vector2i(0, 0), 2);
        Pattern pattern2 = grid.getPatternAtPosition(new Vector2i(2, 2), 2);
        assertEquals(new Pattern(2, new int[][]{{1, 2}, {0, 1}}), pattern1);
        assertEquals(new Pattern(2, new int[][]{{0, 0}, {3, 1}}), pattern2);
    }

    @Test
    public void patternsTest() {
        OverlappingModel overlappingModel = overlappingModel3;
        overlappingModel.patterns.forEach(System.out::println);
    }

    @Test
    public void patternFrequencyTest() {
        OverlappingModel overlappingModel = overlappingModel2;
        for (int i = 0; i < overlappingModel.patterns.size(); i++) {
            System.out.println(overlappingModel.patterns.get(i));
            System.out.println("freq: " + overlappingModel.patternFrequency.get(i));
            System.out.println("------------------");
        }
    }

    @Test
    public void getEdgeTest() {
        Pattern pattern = new Pattern(2, new int[][]{{1, 2}, {0, 1}});
        Stream.of(Direction.values()).forEach(d -> System.out.println(Arrays.toString(pattern.getEdge(d))));
    }

    @Test
    public void findNeighboursTest() {
        OverlappingModel overlappingModel = overlappingModel2;

        OverlappingModel.Neighbours[] neighbours = overlappingModel.patternNeighbours;
        for (int i = 0; i < neighbours.length; i++) {
            OverlappingModel.Neighbours n = neighbours[i];
            System.out.println(overlappingModel.patterns.get(i));
            System.out.println("neighbours: ");
            Stream.of(Direction.values()).forEach(d -> {
                        System.out.println(d.toString());
                        n.neighbours.get(d).forEach(pi ->
                                System.out.println(overlappingModel.patterns.get(pi))
                        );
                    }
            );
            System.out.println("--------------------");
        }
    }

    @Test
    public void solveTest() {
        OverlappingModel overlappingModel = this.overlappingModel3;
        overlappingModel.solve();
        List<List<Integer>> wave = overlappingModel.wave;
        for (int y = 0; y < overlappingModel.outputSize.y; y++) {
            for (int x = 0; x < overlappingModel.outputSize.x; x++) {
                System.out.print(wave.get(x + y * overlappingModel.outputSize.x).get(0) + "  ");
            }
            System.out.println();
        }

        for (int i = 0; i < overlappingModel.patterns.size(); i++) {
            System.out.println("index: " + i);
            System.out.println(overlappingModel.patterns.get(i));
        }
    }

    @Test
    public void t(){
        List<Integer> cell1 = List.of(0,1,2,3);
        List<Integer> cell2 = List.of(0,1,2);
        List<Double> patternFrequency = List.of(0.1,0.42,0.25,0.33);

        Double entropy1 = getEntropy(cell1, patternFrequency);
        Double entropy2 = getEntropy(cell2, patternFrequency);

        System.out.println(entropy1);
        System.out.println(entropy2);

        assertTrue(entropy1 > entropy2);
    }

    private Double getEntropy(List<Integer> cell, List<Double> patternFrequency) {
        double sumOfWeights = cell.stream()
                .map(index -> patternFrequency.get(index))
                .reduce(0d, Double::sum, Double::sum);

        double logSumOfWeights = cell.stream()
                .map(index -> patternFrequency.get(index))
                .reduce(0d, (sum, weight) -> sum + (weight * Math.log(weight)), Double::sum);

        return Math.log(sumOfWeights) - (logSumOfWeights / sumOfWeights);
    }

    private double log2(double value) {
        return Math.log(value) / Math.log(2);
    }

}